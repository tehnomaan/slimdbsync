package eu.miltema.slimdbsync;

import eu.miltema.slimdbsync.pg.PgAdapter;
import eu.miltema.slimorm.*;

import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.*;
import java.lang.reflect.Field;
import java.sql.Statement;

import javax.persistence.*;

public class DatabaseSync {

	private Database db;
	private DatabaseAdapter dbAdapter;
	private SyncContext ctx;
	private Consumer<String> logger = message -> {};
	private List<String> messageElements = new ArrayList<String>();//elements for debug messages
	private boolean dropSequences = true, dropTables = true, dropColumns = true;

	public DatabaseSync(Database db) {
		this.db = db;
		this.dbAdapter = new PgAdapter(db.getSchema());
		this.ctx = new SyncContext();
	}

	public DatabaseSync setLogger(Consumer<String> logger) {
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
		for(Class<?> clazz : entityClasses) {
			EntityProperties eprops = db.getDialect().getProperties(clazz);
			TableDef table = new TableDef();
			table.name = eprops.tableName;
			table.columns = eprops.fields.stream().map(f -> {
				ColumnDef c = new ColumnDef();
				c.name = f.columnName;
				c.isNullable = (f == eprops.idField ? false : isNullable(f.field));
				c.isJson = f.field.isAnnotationPresent(JSon.class);
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
							throw new SchemaUpdateException(ref(clazz, f) + ": table strategy not supported");
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
		}
	}

	private String ref(Class<?> clazz, FieldProperties fprops) {
		return clazz.getSimpleName() + "/" + fprops.field.getName();
	}

	private void loadCurrentSchema() throws Exception {
		ctx.dbSequenceNames = dbAdapter.loadExistingSequenceNames(db);
		ctx.dbTables = dbAdapter.loadExistingTables(db).stream().collect(toMap(def -> def.name, def -> def));
		ctx.dbPrimaryKeys = dbAdapter.loadExistingPrimaryKeys(db);
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
			if (dropColumns) detectRemovedColumns(table, sb);
		});
		if (dropTables) detectRemovedTables(sb);
		if (dropSequences) detectRemovedSequences(sb);
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

	public DatabaseSync dropUnusedSequences(boolean b) {
		this.dropSequences = b;
		return this;
	}
	
	public DatabaseSync dropUnusedTables(boolean b) {
		this.dropTables = b;
		return this;
	}
	
	public DatabaseSync dropUnusedColumns(boolean b) {
		this.dropColumns = b;
		return this;
	}

	public DatabaseSync dropUnusedElements(boolean b) {
		dropColumns = dropSequences = dropTables = b;
		return this;
	}
}
