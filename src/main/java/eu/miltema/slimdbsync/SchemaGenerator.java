package eu.miltema.slimdbsync;

import eu.miltema.slimdbsync.pg.PgAdapter;
import eu.miltema.slimorm.*;

import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.*;
import java.lang.reflect.Field;
import java.sql.Statement;

import javax.persistence.*;

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
			EntityProperties eprops = db.getDialect().getProperties(clazz);
			TableDef table = new TableDef();
			table.name = eprops.tableName;
			table.columns = eprops.fields.stream().map(f -> {
				ColumnDef c = new ColumnDef();
				c.name = f.columnName;
				c.isNullable = (f == eprops.idField ? false : isNullable(f.field));
				c.isJson = f.field.isAnnotationPresent(JSon.class);
				c.isPrimaryKey = (f == eprops.idField);
				c.isIdentity = (f.field.isAnnotationPresent(GeneratedValue.class) ? f.field.getAnnotation(GeneratedValue.class).strategy() == GenerationType.IDENTITY : false);
				c.type = (c.isJson ? dbAdapter.sqlTypeForJSon() : dbAdapter.sqlType(f.fieldType));
				c.sourceSequence = getSourceSequence(eprops, f);
				c.columnDefinitionOverride = (f.field.isAnnotationPresent(Column.class) ? f.field.getAnnotation(Column.class).columnDefinition() : null);
				if ("".equals(c.columnDefinitionOverride))
					c.columnDefinitionOverride = null;
				if (f.field.isAnnotationPresent(GeneratedValue.class))
					switch (f.field.getAnnotation(GeneratedValue.class).strategy()) {
					case IDENTITY:
						if (!dbAdapter.supportsIdentityStrategy())
							throw new SchemaUpdateException(ref(clazz, f) + ": identity strategy not supported");
						c.isIdentity = true;
						break;
					case TABLE:
						throw new SchemaUpdateException(ref(clazz, f) + ": table strategy not supported");
					default:
						break;
					}

				if (c.sourceSequence != null)
					ctx.modelSequenceNames.add(c.sourceSequence);

				return c;
			}).collect(toMap(c -> c.name, c -> c));
			ctx.modelTables.put(table.name, table);
			if (eprops.idField != null)
				ctx.modelPrimaryKeys.put(table.name, new PrimaryKeyDef(table.name, eprops.idField.columnName, null));
		}

		initModelForeignKeys(entityClasses);
	}

	private void initModelForeignKeys(Class<?>[] entityClasses) {
		ctx.modelForeignKeys = new HashMap<>();
		for(Class<?> clazz : entityClasses) {
			EntityProperties eprops = db.getDialect().getProperties(clazz);
			eprops.fields.stream().forEach(f -> {
				if (f.field.isAnnotationPresent(ManyToOne.class)) {
					Class<?> targetClass = f.field.getAnnotation(ManyToOne.class).targetEntity();
					if (!f.fieldType.getPackage().getName().startsWith("java"))
						throw new SchemaUpdateException(ref(clazz, f) + ": foreign key must be elementary type or String");
					if (targetClass == null)
						throw new SchemaUpdateException(ref(clazz, f) + ": missing targetEntity in @ManyToOne");
					EntityProperties target = db.getDialect().getProperties(targetClass);
					if (target == null)
						throw new SchemaUpdateException(ref(clazz, f) + ": @ManyToOne target class " + targetClass.getName() + " not registered as entity with SchemaGenerator");
					if (target.idField == null)
						throw new SchemaUpdateException(ref(clazz, f) + ": @ManyToOne target class " + targetClass.getName() + " does not declare id-field");
					ForeignKeyDef fdef = new ForeignKeyDef(eprops.tableName, f.columnName, target.tableName, target.idField.columnName, null);
					ctx.modelForeignKeys.put(fdef.localTable + "/" + fdef.localColumn, fdef);
				}
			});
		}
	}

	private String ref(Class<?> clazz, FieldProperties fprops) {
		return clazz.getSimpleName() + "/" + fprops.field.getName();
	}

	private void loadCurrentSchema() throws Exception {
		ctx.dbSequenceNames = dbAdapter.loadCurrentSequenceNames(db);
		ctx.dbTables = dbAdapter.loadCurrentTables(db).stream().collect(toMap(def -> def.name, def -> def));
		ctx.dbPrimaryKeys = dbAdapter.loadCurrentPrimaryKeys(db).stream().collect(toMap(pk -> pk.table, pk -> pk));
		ctx.dbForeignKeys = dbAdapter.loadCurrentForeignKeys(db).stream().collect(toMap(fk -> fk.localTable + "/" + fk.localColumn, fk -> fk));
	}

	private String getSourceSequence(EntityProperties e, FieldProperties f) {
		if (f.field.isAnnotationPresent(GeneratedValue.class)) {
			GeneratedValue gv = f.field.getAnnotation(GeneratedValue.class);
			if (gv.strategy() == GenerationType.AUTO || gv.strategy() == GenerationType.SEQUENCE) {
				String seqName = gv.generator();
				if (seqName == null || seqName.trim().isEmpty())
					return dbAdapter.getDefaultSequenceName(e.tableName, f.columnName);
				else return seqName.trim();
			}
		}
		else if (e.idField == f && !f.field.isAnnotationPresent(Id.class))// this is an id-field without @Id and @GeneratedValue
			return dbAdapter.getDefaultSequenceName(e.tableName, f.columnName);
		return null;
		
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

	private void detectRemovedPrimaryKeys(StringBuilder sb) {
		for(PrimaryKeyDef pk : ctx.dbPrimaryKeys.values()) {
			TableDef table = ctx.modelTables.get(pk.table);
			if (table == null)
				continue;//a table was dropped: pk will be implicitly cascade-dropped
			if (!table.columns.containsKey(pk.column))
				continue;//pk column was removed; pk will be implicitly cascade-dropped
			if (table.columns.get(pk.column).isPrimaryKey)
				continue;//column is still primary key; don' drop the constraint
			sb.append(dbAdapter.dropPrimaryKey(pk.table, pk.column, pk.constraintName));
		}
	}

	private String getPrimaryKeyColumn(TableDef table) {
		return table.columns.values().stream().filter(c -> c.isPrimaryKey).map(c -> c.name).findAny().orElse(null);
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
			filter(col -> {
				ColumnDef col2 = existingCols.get(col.name);
				return (col.isNullable != col2.isNullable || col.isJson != col2.isJson ||
						!Objects.equals(col.sourceSequence, col2.sourceSequence) || !Objects.equals(col.type, col2.type));
			}).
			forEach(col -> sb.append(dbAdapter.alterColumn(newTable.name, col)));
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

	private boolean isNullable(Field field) {
		if (field.isAnnotationPresent(Id.class))
			return false;
		if (field.isAnnotationPresent(Column.class))
			return field.getAnnotation(Column.class).nullable();
		Class<?> type = field.getType();
		return !(type == boolean.class || type == short.class || type == int.class || type == long.class || type == double.class || type == float.class);
	}

	public SchemaGenerator dropUnused(boolean b) {
		dropUnused = b;
		return this;
	}
}
