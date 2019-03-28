package eu.miltema.slimdbsync.pg;

import static java.util.stream.Collectors.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import com.google.gson.Gson;

import eu.miltema.slimdbsync.*;
import eu.miltema.slimdbsync.def.*;
import eu.miltema.slimorm.Database;

/**
 * Adapter for PostgreSQL database.
 * See https://www.postgresql.org/docs/9.4/catalog-pg-constraint.html
 *
 * @author Margus
 */
public class PgAdapter implements DatabaseAdapter {

	private static final String ENDL = "\r\n";

	private String schema;
	private Collection<UniqueDef> uniques = new ArrayList<>();
	private Collection<CheckDef> checks = new ArrayList<>();

	public PgAdapter(String schema) {
		this.schema = schema;
	}

	@Override
	public Set<String> loadCurrentSequenceNames(Database db) throws Exception {
		return db.where("sequence_schema=?", db.getSchema()).stream(PgSequence.class).map(seq -> seq.sequenceName).collect(toSet());
	}

	@Override
	public Collection<TableDef> loadCurrentTables(Database db) throws Exception {
		List<TableDef> tables = db.where("schemaname=?", schema).stream(PgTable.class).map(t -> {
			TableDef table = new TableDef();
			table.name = t.tablename;
			return table;
		}).collect(toList());
		for(TableDef table : tables)
			table.columns = loadExistingColumns(db, table.name);
		return tables;
	}

	private Map<String, ColumnDef> loadExistingColumns(Database db, String tablename) throws Exception {
		Map<String, ColumnDef> mapCols = db.sql("SELECT * FROM information_schema.columns WHERE table_schema=? AND table_name=?", schema, tablename).
				stream(PgColumn.class).map(r -> {
					ColumnDef col = new ColumnDef();
					col.name = r.name;
					col.type = r.dataType.toLowerCase();
					col.isNullable = r.isNullable;
					col.isJson = "json".equalsIgnoreCase(r.dataType);
					col.ordinal = r.ordinalPosition;
					if (r.defaultValue != null && r.defaultValue.startsWith("nextval('") && r.defaultValue.endsWith("'::regclass)"))
						col.sourceSequence = r.defaultValue.substring(9, r.defaultValue.length() - 12);
					return col;
				}).collect(toMap(cdef -> cdef.name, cdef -> cdef));
		String[] colnames = mapCols.values().stream().sorted((c1, c2) -> (c1.ordinal < c2.ordinal ? -1 : 1)).map(c -> c.name).toArray(String[]::new);

		String sql = "SELECT conname, conrelid::regclass, conkey::character varying FROM pg_constraint " +
				"WHERE connamespace::regnamespace::character varying=? AND conrelid::regclass::character varying=? AND contype=?";
		uniques.addAll(db.sql(sql, schema, tablename, "u").stream(PgUnique.class).map(pgu -> {
			UniqueDef udef = new UniqueDef();
			udef.name = pgu.conname;
			udef.tableName = tablename;
			int[] ordinals = new Gson().fromJson(pgu.conkey.replace('{', '[').replace('}', ']'), int[].class);
			udef.columns = Arrays.stream(ordinals).mapToObj(o -> colnames[o - 1]).toArray(String[]::new);
			return udef;
		}).collect(toList()));

		sql = "SELECT conname, conrelid::regclass, connamespace::regnamespace, conkey::character varying, consrc FROM pg_constraint " +
				"WHERE connamespace::regnamespace::character varying=? AND conrelid::regclass::character varying=? AND contype=?";
		checks.addAll(db.sql(sql, schema, tablename, "c").stream(PgCheck.class).map(pgc -> {
			int[] ordinals = new Gson().fromJson(pgc.conkey.replace('{', '[').replace('}', ']'), int[].class);
			CheckDef cdef = new CheckDef(pgc.conname, tablename, colnames[ordinals[0] - 1]);
			// Cannot use simple Scanner.findAll-method, since it is not available in Java 1.8
			try(Scanner s = new Scanner(pgc.consrc)) {
				String pattern = "'([a-zA-Z0-9_]+)'";
				Stream.Builder<String> builder = Stream.builder();
		        while (s.findInLine(pattern) != null)
		            builder.accept(s.match().group(1));
				cdef.validValues = builder.build().toArray(String[]::new);
			}
			return cdef;
		}).collect(toList()));
		return mapCols;
	}

	@Override
	public Collection<PrimaryKeyDef> loadCurrentPrimaryKeys(Database db) throws Exception {
		final String sql = "SELECT tc.table_schema, tc.table_name, kc.column_name, tc.constraint_catalog db_name, tc.constraint_name" + 
				" FROM information_schema.table_constraints tc, information_schema.key_column_usage kc" + 
				" WHERE tc.constraint_type = ? AND tc.constraint_catalog=current_database() AND kc.table_name = tc.table_name and kc.table_schema = tc.table_schema AND kc.constraint_name = tc.constraint_name";
		return db.sql(sql, "PRIMARY KEY").stream(PgPrimaryKey.class).map(r -> new PrimaryKeyDef(r.tableName, r.columnName, r.constraintName)).collect(toList());
	}

	@Override
	public Collection<ForeignKeyDef> loadCurrentForeignKeys(Database db) throws Exception {
		final String sql = "SELECT tc.table_schema, tc.constraint_name, tc.table_name, kcu.column_name, ccu.table_schema AS foreign_table_schema, ccu.table_name AS foreign_table, ccu.column_name AS foreign_column " + 
				"FROM information_schema.table_constraints AS tc " + 
				"  JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema " + 
				"  JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name AND ccu.table_schema = tc.table_schema " + 
				"WHERE tc.constraint_type = ?";
		return db.sql(sql, "FOREIGN KEY").stream(PgForeignKey.class).map(r -> new ForeignKeyDef(r.tableName, r.columnName, r.foreignTable, r.foreignColumn, r.constraintName)).collect(toList());
	}

	@Override
	public Collection<UniqueDef> loadCurrentUniques(Database db) throws Exception {
		return uniques;
	}

	@Override
	public Collection<CheckDef> loadCurrentChecks(Database db) throws Exception {
		return checks;
	}

	@Override
	public Collection<IndexDef> loadCurrentIndexes(Database db) throws Exception {
		final String sql = "SELECT * FROM pg_indexes WHERE schemaname=?";
		return db.sql(sql, schema).stream(PgIndex.class).map(pgi -> {
			IndexDef idef = new IndexDef();
			idef.name = pgi.indexname;
			idef.tableName = pgi.tablename;
			idef.isUniqueIndex = pgi.indexdef.toUpperCase().contains("CREATE UNIQUE INDEX");
			idef.columns = pgi.indexdef.substring(pgi.indexdef.indexOf('(') + 1,pgi.indexdef.indexOf(')')).split(",");
			return idef;
		}).filter(idef -> !idef.isUniqueIndex).collect(toList());//ignore database-created unique indexes (for pkey & chec constraints)
	}

	@Override
	public String createTableWithColumns(TableDef tableDef) {
		String columns = tableDef.columns.values().stream().map(coldef -> getColumnDefinition(coldef)).collect(joining("," + ENDL + "  "));
		return "CREATE TABLE \"" + tableDef.name + "\"(" + ENDL + "  " + columns + ENDL + ");" + ENDL;
	}

	public String getColumnDefinition(ColumnDef cdef) {
		if (cdef.columnDefinitionOverride != null)
			return "\"" + cdef.name + "\" " + cdef.columnDefinitionOverride;
		return "\"" + cdef.name + "\" " + cdef.type + (cdef.isNullable ? "" : " NOT NULL") + (cdef.sourceSequence == null ? "" : " " + "DEFAULT nextval('" + cdef.sourceSequence + "'::regclass)");
	}

	@Override
	public String addColumn(String tableName, ColumnDef column) {
		return "ALTER TABLE \"" + tableName + "\" ADD COLUMN " + getColumnDefinition(column) + ";" + ENDL;
	}

	@Override
	public String dropColumn(String tableName, String colname) {
		return "ALTER TABLE \"" + tableName + "\" DROP COLUMN \"" + colname + "\";" + ENDL;
	}

	@Override
	public String sqlType(Class<?> javaType) throws SchemaUpdateException {
		if (javaType == boolean.class || javaType == Boolean.class) return "boolean";
		else if (javaType == short.class || javaType == Short.class) return "smallint";
		else if (javaType == int.class || javaType == Integer.class) return "integer";
		else if (javaType == long.class || javaType == Long.class) return "bigint";
		else if (javaType == float.class || javaType == Float.class) return "real";
		else if (javaType == double.class || javaType == Double.class) return "double precision";
		else if (javaType == byte.class || javaType == Byte.class) return "smallint";
		else if (javaType == byte[].class) return "bytea";
		else if (javaType == BigDecimal.class) return "numeric";
		else if (javaType == String.class) return "character varying";
		else if (javaType == Timestamp.class) return "timestamp without time zone";
		else if (javaType == Instant.class) return "timestamp without time zone";
		else if (javaType == ZonedDateTime.class) return "timestamp with time zone";
		else if (javaType == LocalDate.class) return "date";
		else if (javaType == LocalDateTime.class) return "timestamp without time zone";
		else if (javaType.isEnum()) return "character varying";
		else throw new SchemaUpdateException("Unsupported type " + javaType.getSimpleName());
	}

	@Override
	public String sqlTypeForJSon() {
		return "json";
	}

	@Override
	public String getDefaultSequenceName(String tablename, String columnName) {
		return tablename + "_" + columnName + "_seq";
	}

	@Override
	public Object createSequence(String sequenceName) {
		return "CREATE SEQUENCE " + sequenceName + " INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1;" + ENDL;
	}

	@Override
	public Object dropSequence(String sequenceName) {
		return "DROP SEQUENCE IF EXISTS " + sequenceName + " CASCADE;" + ENDL;//sometimes, sequence is already cascade-dropped with related table 
	}

	@Override
	public String dropTable(String tablename) {
		return "DROP TABLE \"" + tablename + "\" CASCADE;" + ENDL;
	}

	@Override
	public boolean supportsIdentityStrategy() {
		return false;
	}

	@Override
	public String dropPrimaryKey(String tableName, String columnName, String constraintName) {
		return "ALTER TABLE \"" + tableName + "\" DROP CONSTRAINT " + constraintName + ";" +ENDL;
	}

	@Override
	public String addPrimaryKey(String tableName, String columnName) {
		return "ALTER TABLE \"" + tableName + "\" ADD PRIMARY KEY (\"" + columnName + "\");" + ENDL;
	}

	@Override
	public String dropForeignKey(String localTable, String localColumn, String constraintName) {
		return "ALTER TABLE \"" + localTable + "\" DROP CONSTRAINT \"" + constraintName + "\";" + ENDL;
	}

	@Override
	public String createForeignKey(ForeignKeyDef foreignKeyDef) {
		return "ALTER TABLE \"" + foreignKeyDef.localTable + "\" ADD FOREIGN KEY (\"" + foreignKeyDef.localColumn +
				"\") REFERENCES \"" + foreignKeyDef.foreignTable + "\"(\""+ foreignKeyDef.foreignColumn + "\");" + ENDL;
	}

	@Override
	public String createUnique(UniqueDef u) {
		String cname = u.tableName + "_" + Arrays.stream(u.columns).collect(joining("_"));
		return "ALTER TABLE \"" + u.tableName + "\" ADD CONSTRAINT " + cname +
				" UNIQUE (" + Arrays.stream(u.columns).map(c -> "\"" + c + "\"").collect(joining(", ")) + ");" + ENDL;
	}

	@Override
	public String dropUnique(UniqueDef u) {
		return "ALTER TABLE \"" + u.tableName + "\" DROP CONSTRAINT " + u.name + ";" + ENDL;
	}

	@Override
	public String createCheck(CheckDef checkDef) {
		String vals = Arrays.stream(checkDef.validValues).map(v -> "'" + v + "'").collect(joining(","));
		return "ALTER TABLE \"" + checkDef.tableName + "\" ADD CHECK (\"" + checkDef.columnName + "\" IN (" + vals + "));" + ENDL;
	}

	@Override
	public String dropCheck(CheckDef checkDef) {
		return "ALTER TABLE \"" + checkDef.tableName + "\" DROP CONSTRAINT " + checkDef.name + ";" + ENDL;
	}

	@Override
	public String createIndex(IndexDef indexDef) {
		return "CREATE INDEX ON \"" + indexDef.tableName + "\" (" + Arrays.stream(indexDef.columns).map(c -> "\"" + c + "\"").collect(joining(", ")) + ");" + ENDL;
	}

	@Override
	public String dropIndex(IndexDef indexDef) {
		return "DROP INDEX IF EXISTS " + indexDef.name + ";" + ENDL;
	}

	@Override
	public String alterColumnType(String tableName, String columnName, String sqlType) {
		return "ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + columnName + "\" TYPE " + sqlType + ";" + ENDL;
	}

	@Override
	public String alterColumnNullability(String tableName, String columnName, boolean isNullable) {
		return "ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + columnName + (isNullable ? "\" DROP NOT NULL;" : "\" SET NOT NULL;") + ENDL;
	}

	@Override
	public String alterColumnDefaultValue(String tableName, String columnName, String sourceSequence) {
		return "ALTER TABLE \"" + tableName + "\" ALTER COLUMN \"" + columnName + "\" SET DEFAULT " + (sourceSequence == null ? "null" : " " + "nextval('" + sourceSequence + "'::regclass)") + ";" + ENDL;
	}
}
