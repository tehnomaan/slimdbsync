package eu.miltema.slimdbsync.pg;

import static java.util.stream.Collectors.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import eu.miltema.slimdbsync.*;
import eu.miltema.slimorm.Database;

/**
 * Adapter for PostgreSQL database
 * @author Margus
 */
public class PgAdapter implements DatabaseAdapter {

	private static final String ENDL = "\r\n";

	private String schema;

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
		return db.sql("SELECT * FROM information_schema.columns WHERE table_schema=? AND table_name=?", schema, tablename).
				stream(PgColumn.class).map(r -> {
					ColumnDef col = new ColumnDef();
					col.name = r.name;
					col.type = r.dataType.toLowerCase();
					col.isNullable = r.isNullable;
					col.isJson = "json".equalsIgnoreCase(r.dataType);
					if (r.defaultValue != null && r.defaultValue.startsWith("nextval('") && r.defaultValue.endsWith("'::regclass)"))
						col.sourceSequence = r.defaultValue.substring(9, r.defaultValue.length() - 12);
					return col;
				}).collect(toMap(cdef -> cdef.name, cdef -> cdef));
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
	public String createTableWithColumns(TableDef tableDef) {
		String columns = tableDef.columns.values().stream().map(coldef -> getColumnDefinition(coldef, false)).collect(joining("," + ENDL + "  "));
		return "CREATE TABLE \"" + tableDef.name + "\"(" + ENDL + "  " + columns + ENDL + ");" + ENDL;
	}

	public String getColumnDefinition(ColumnDef cdef, boolean includeTypeWord) {
		if (cdef.columnDefinitionOverride != null)
			return "\"" + cdef.name + "\" " + (includeTypeWord ? "TYPE " : "") + cdef.columnDefinitionOverride;
		return "\"" + cdef.name + "\" " + (includeTypeWord ? "TYPE " : "") + cdef.type + (cdef.isNullable ? "" : " NOT NULL") + (cdef.sourceSequence == null ? "" : " " + "DEFAULT nextval('" + cdef.sourceSequence + "'::regclass)");
	}

	@Override
	public String addColumn(String tableName, ColumnDef column) {
		return "ALTER TABLE \"" + tableName + "\" ADD COLUMN " + getColumnDefinition(column, false) + ";" + ENDL;
	}

	@Override
	public String alterColumn(String tableName, ColumnDef column) {
		return "ALTER TABLE \"" + tableName + "\" ALTER COLUMN " + getColumnDefinition(column, true) + ";" + ENDL;
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
		return "ALTER TABLE \"" + localTable + "\" DROP CONSTRAINT (\"" + constraintName + "\");" + ENDL;
	}

	@Override
	public String createForeignKey(ForeignKeyDef foreignKeyDef) {
		return "ALTER TABLE \"" + foreignKeyDef.localTable + "\" ADD FOREIGN KEY (\"" + foreignKeyDef.localColumn +
				"\") REFERENCES \"" + foreignKeyDef.foreignTable + "\"(\""+ foreignKeyDef.foreignColumn + "\");" + ENDL;
	}
}
