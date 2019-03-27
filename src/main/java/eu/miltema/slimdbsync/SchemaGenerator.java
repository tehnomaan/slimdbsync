package eu.miltema.slimdbsync;

import eu.miltema.slimdbsync.def.ColumnDef;
import eu.miltema.slimdbsync.def.ForeignKeyDef;
import eu.miltema.slimdbsync.def.ModelColumnDef;
import eu.miltema.slimdbsync.def.PrimaryKeyDef;
import eu.miltema.slimdbsync.def.TableDef;
import eu.miltema.slimdbsync.def.UniqueDef;
import eu.miltema.slimdbsync.pg.PgAdapter;
import eu.miltema.slimorm.*;

import java.util.*;
import java.util.function.Consumer;

import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static java.util.stream.Collectors.*;
import java.sql.Statement;


public class SchemaGenerator {

	private Database db;
	private DatabaseAdapter dbAdapter;
	private SyncContext ctx;
	private Consumer<String> logger = message -> {};
	private List<String> messageElements = new ArrayList<String>();//elements for debug messages
	private boolean dropUnused = true;

	public SchemaGenerator(Database db) {
		this.db = db;
		this.dbAdapter = new PgAdapter(db.getSchema());
		this.ctx = new SyncContext();
	}

	public SchemaGenerator setLogger(Consumer<String> logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * Synchronize database tables according to entity classes
	 * @param entityClasses entity classes
	 * @throws Exception 
	 */
	public void sync(Class<?> ... entityClasses) throws Exception {
		initModelTables(entityClasses);
		loadCurrentSchema();
		StringBuilder sb = new StringBuilder();
		detectChanges(sb);
		applyChanges(sb.toString());
	}

	private void initModelTables(Class<?>[] entityClasses) throws SchemaUpdateException {
		ctx.modelSequenceNames = new HashSet<>();
		ctx.modelTables = new HashMap<>();
		ctx.modelPrimaryKeys = new HashMap<>();
		for(Class<?> clazz : entityClasses) {
			EntityProperties eprop = db.getDialect().getProperties(clazz);
			TableDef table = new TableDef();
			table.name = eprop.tableName;
			table.columns = eprop.fields.stream().map(fprop -> {
				ModelColumnDef c = new ModelColumnDef(eprop, fprop, dbAdapter);
				if (c.sourceSequence != null)
					ctx.modelSequenceNames.add(c.sourceSequence);
				return c;
			}).collect(toMap(c -> c.name, c -> c));
			ctx.modelTables.put(table.name, table);
			if (eprop.idField != null)
				ctx.modelPrimaryKeys.put(table.name, new PrimaryKeyDef(table.name, eprop.idField.columnName, null));
		}
		initModelForeignKeys(entityClasses);
		initModelUniques(entityClasses);
	}

	private void initModelForeignKeys(Class<?>[] entityClasses) {
		ctx.modelForeignKeys = new HashMap<>();
		for(Class<?> clazz : entityClasses) {
			EntityProperties eprops = db.getDialect().getProperties(clazz);
			eprops.fields.stream().forEach(f -> {
				ModelColumnDef coldef = (ModelColumnDef) ctx.modelTables.get(eprops.tableName).columns.get(f.columnName);
				if (coldef.isForeignKey) {
					Class<?> targetClass = f.fieldType;
					EntityProperties target = db.getDialect().getProperties(targetClass);
					EntityProperties targetProps = db.getDialect().getProperties(targetClass);
					if (targetProps == null)
						throw new SchemaUpdateException(f.field, ": @ManyToOne target class " + targetClass.getName() + " not registered with SchemaGenerator");
					if (target.idField == null)
						throw new SchemaUpdateException(f.field, ": @ManyToOne target class " + targetClass.getName() + " does not declare id-field");
					coldef.type = ctx.modelTables.get(targetProps.tableName).columns.values().stream().filter(fcoldef -> fcoldef.isPrimaryKey()).map(fcoldef -> fcoldef.type).findAny().orElse(null);
					ForeignKeyDef fdef = new ForeignKeyDef(eprops.tableName, f.columnName, target.tableName, target.idField.columnName, null);
					ctx.modelForeignKeys.put(fdef.localTable + "/" + fdef.localColumn, fdef);
				}
			});
		}
	}

	private void initModelUniques(Class<?>[] entityClasses) {
		ctx.modelUniques = new HashMap<>();
		for(Class<?> clazz : entityClasses) {
			EntityProperties eprops = db.getDialect().getProperties(clazz);
			// Add fields with @Column(unique=true)
			ctx.modelTables.get(eprops.tableName).columns.values().stream().map(coldef -> (ModelColumnDef) coldef).filter(coldef -> coldef.isUnique).forEach(coldef -> {
				UniqueDef udef = new UniqueDef();
				udef.tableName = eprops.tableName;
				udef.columns = new String[] {coldef.name};
				ctx.modelUniques.put(udef.toString(), udef);
			});
			// Add fields with @Table(uniqueConstraints = @UniqueConstraint(columnNames= {"abc", "xyz"}))
			if (clazz.isAnnotationPresent(Table.class)) {
				UniqueConstraint[] uca = clazz.getAnnotation(Table.class).uniqueConstraints();
				if (uca != null)
					for(UniqueConstraint uc : uca)
						if (uc.columnNames() != null && uc.columnNames().length > 0) {
							UniqueDef udef = new UniqueDef();
							udef.tableName = eprops.tableName;
							udef.columns = uc.columnNames();
							ctx.modelUniques.put(udef.toString(), udef);
						}
			}
		}
	}

	private void loadCurrentSchema() throws Exception {
		ctx.dbSequenceNames = dbAdapter.loadCurrentSequenceNames(db);
		ctx.dbTables = dbAdapter.loadCurrentTables(db).stream().collect(toMap(def -> def.name, def -> def));
		ctx.dbPrimaryKeys = dbAdapter.loadCurrentPrimaryKeys(db).stream().collect(toMap(pk -> pk.table, pk -> pk));
		ctx.dbForeignKeys = dbAdapter.loadCurrentForeignKeys(db).stream().collect(toMap(fk -> fk.localTable + "/" + fk.localColumn, fk -> fk));
		ctx.dbUniques = dbAdapter.loadCurrentUniques(db).stream().collect(toMap(u -> u.toString(), u -> u));
	}

	private void detectChanges(StringBuilder sb) {
		detectNewSequences(sb);
		detectNewTables(sb);
		ctx.modelTables.values().stream().filter(table -> ctx.dbTables.containsKey(table.name)).forEach(table -> {
			detectNewColumns(table, sb);
			detectChangedColumns(table, sb);
			if (dropUnused) detectRemovedColumns(table, sb);
		});
		detectNewPrimaryKeys(sb);
		detectRemovedPrimaryKeys(sb);
		detectNewForeignKeys(sb);
		detectRemovedForeignKeys(sb);
		detectNewUniques(sb);
		detectRemovedUniques(sb);
		if (dropUnused) detectRemovedTables(sb);
		if (dropUnused) detectRemovedSequences(sb);
	}

	private void detectNewForeignKeys(StringBuilder sb) {
		ctx.modelForeignKeys.keySet().stream().
			filter(mfname -> !ctx.dbForeignKeys.containsKey(mfname)).
			map(mfname -> ctx.modelForeignKeys.get(mfname)).
			forEach(mfk -> sb.append(dbAdapter.createForeignKey(mfk)));
	}

	private void detectRemovedForeignKeys(StringBuilder sb) {
		ctx.dbForeignKeys.keySet().stream().
			filter(dbfname -> !ctx.modelForeignKeys.containsKey(dbfname)).
			map(dbfname -> ctx.dbForeignKeys.get(dbfname)).
			forEach(dbf -> sb.append(dbAdapter.dropForeignKey(dbf.localTable, dbf.localColumn, dbf.constraintName)));
	}

	private void detectNewUniques(StringBuilder sb) {
		ctx.modelUniques.keySet().stream().
			filter(uname -> !ctx.dbUniques.containsKey(uname)).
			map(uname -> ctx.modelUniques.get(uname)).
			forEach(u -> sb.append(dbAdapter.createUnique(u)));
	}

	private void detectRemovedUniques(StringBuilder sb) {
		ctx.dbUniques.keySet().stream().
			filter(uname -> !ctx.modelUniques.containsKey(uname)).
			map(uname -> ctx.dbUniques.get(uname)).
			forEach(u -> sb.append(dbAdapter.dropUnique(u)));
	}

	private void detectRemovedPrimaryKeys(StringBuilder sb) {
		for(PrimaryKeyDef pk : ctx.dbPrimaryKeys.values()) {
			TableDef table = ctx.modelTables.get(pk.table);
			if (table == null)
				continue;//a table was dropped: pk will be implicitly cascade-dropped
			if (!table.columns.containsKey(pk.column))
				continue;//pk column was removed; pk will be implicitly cascade-dropped
			if (table.columns.get(pk.column).isPrimaryKey())
				continue;//column is still primary key; don' drop the constraint
			sb.append(dbAdapter.dropPrimaryKey(pk.table, pk.column, pk.constraintName));
		}
	}

	private String getPrimaryKeyColumn(TableDef table) {
		return table.columns.values().stream().filter(c -> c.isPrimaryKey()).map(c -> c.name).findAny().orElse(null);
	}

	private void detectNewSequences(StringBuilder sb) {
		ctx.modelSequenceNames.stream().
			filter(s -> !ctx.dbSequenceNames.contains(s)).
			peek(s -> messageElements.add(s)).
			forEach(s -> sb.append(dbAdapter.createSequence(s)));
		logElementsMessage("Added sequences ");
	}

	private void detectNewTables(StringBuilder sb) {
		ctx.modelTables.values().stream().
			filter(table -> !ctx.dbTables.containsKey(table.name)).
			peek(table -> messageElements.add(table.name)).
			forEach(table -> sb.append(dbAdapter.createTableWithColumns(table)));
		logElementsMessage("Added tables ");
	}

	private void detectNewColumns(TableDef newTable, StringBuilder sb) {
		Map<String, ColumnDef> existingCols = ctx.dbTables.get(newTable.name).columns;
		newTable.columns.values().stream().
			filter(col -> !existingCols.containsKey(col.name)).
			peek(col -> messageElements.add(col.name)).
			forEach(col -> sb.append(dbAdapter.addColumn(newTable.name, col)));
		logElementsMessage("Added " + newTable.name + " columns ");
	}

	private void detectChangedColumns(TableDef newTable, StringBuilder sb) {
		Map<String, ColumnDef> existingCols = ctx.dbTables.get(newTable.name).columns;
		newTable.columns.values().stream().
			filter(col -> existingCols.containsKey(col.name)).
			filter(col -> col.columnDefinitionOverride == null). // if manual column definition is present, then we won't manage changes, because new/existing comparison is inaccurate
			forEach(col -> {
				ColumnDef col2 = existingCols.get(col.name);
				if (!Objects.equals(col.type, col2.type))
					sb.append(dbAdapter.alterColumnType(newTable.name, col.name, col.type));
				if (col.isNullable != col2.isNullable)
					sb.append(dbAdapter.alterColumnNullability(newTable.name, col.name, col.isNullable));
				if (!Objects.equals(col.sourceSequence, col2.sourceSequence))
					sb.append(dbAdapter.alterColumnDefaultValue(newTable.name, col.name, col.sourceSequence));
			});
	}

	private void detectRemovedColumns(TableDef newTable, StringBuilder sb) {
		ctx.dbTables.get(newTable.name).columns.keySet().stream().
			filter(col -> !newTable.columns.containsKey(col)).
			peek(col -> messageElements.add(col)).
			forEach(col -> sb.append(dbAdapter.dropColumn(newTable.name, col)));
		logElementsMessage("Removed " + newTable.name + " columns ");
	}

	private void detectNewPrimaryKeys(StringBuilder sb) {
		ctx.modelTables.values().stream().forEach(table -> {
			String pkColumn = getPrimaryKeyColumn(table);
			if (pkColumn != null) {
				PrimaryKeyDef existingKey = ctx.dbPrimaryKeys.get(table.name);
				if (existingKey == null || !Objects.equals(existingKey.column, pkColumn))
					sb.append(dbAdapter.addPrimaryKey(table.name, pkColumn));
			}
		});
	}

	private void detectRemovedTables(StringBuilder sb) {
		ctx.dbTables.keySet().stream().
			filter(table -> !ctx.modelTables.containsKey(table)).
			peek(table -> messageElements.add(table)).
			forEach(table -> sb.append(dbAdapter.dropTable(table)));
		logElementsMessage("Removed tables ");
	}

	private void detectRemovedSequences(StringBuilder sb) {
		ctx.dbSequenceNames.stream().
			filter(seq -> !ctx.modelSequenceNames.contains(seq)).
			peek(seq -> messageElements.add(seq)).
			forEach(seq -> sb.append(dbAdapter.dropSequence(seq)));
		logElementsMessage("Removed sequences ");
	}

	private void logElementsMessage(String messagePrefix) {
		if (messageElements.isEmpty())
			return;
		logger.accept(messagePrefix + messageElements.stream().collect(joining(", ")));
		messageElements.clear();
	}

	protected void applyChanges(String ddlChanges) throws Exception {
		db.transaction((db, connection) -> {
			try(Statement stmt = connection.createStatement()) {
				stmt.executeUpdate(ddlChanges);
			}
			return null;
		});
	}

	public SchemaGenerator dropUnused(boolean b) {
		dropUnused = b;
		return this;
	}
}
