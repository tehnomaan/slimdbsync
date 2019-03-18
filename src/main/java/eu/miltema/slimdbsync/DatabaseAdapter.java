package eu.miltema.slimdbsync;

import java.util.*;

import eu.miltema.slimorm.Database;

public interface DatabaseAdapter {

	/**
	 * Load current sequences from database
	 * @param db database link
	 * @return set of sequence names
	 * @throws Exception when any error occurs
	 */
	Set<String> loadExistingSequenceNames(Database db) throws Exception;

	/**
	 * Load current table definitions (incl column definitions) from database
	 * @param db database link
	 * @return table definitions
	 * @throws Exception when any error occurs
	 */
	Collection<TableDef> loadExistingTables(Database db) throws Exception;

	/**
	 * Load current primary key constraints from database
	 * @param db database link
	 * @return primary key definitions
	 * @throws Exception when any error occurs
	 */
	Collection<PrimaryKeyDef> loadExistingPrimaryKeys(Database db) throws Exception;

	/**
	 * This method provides ALTER TABLE statement for adding a column
	 * @param tableName table name
	 * @param column column definition
	 * @return DROP column clause, for example "DROP COLUMN mycolumn"
	 */
	String addColumn(String tableName, ColumnDef column);

	/**
	 * This method provides ALTER TABLE statement for dropping a column
	 * @param tableName table name
	 * @param colname column name
	 * @return DROP column clause, for example "DROP COLUMN mycolumn"
	 */
	String dropColumn(String tableName, String colname);

	/**
	 * This method provides for ALTER TABLE statement
	 * @param tableName table name
	 * @param addColumns clause for column definition
	 * @param dropColumns column names to drop
	 * @return ALTER TABLE statement
	 */
	String alterTable(String tableName, List<String> addColumns, List<String> dropColumns);

	/**
	 * Get default sequence name for database column
	 * @param columnName column name
	 * @return sequence name
	 */
	String getDefaultSequenceName(String tablename, String columnName);
	Object createSequence(String sequenceName);
	Object dropSequence(String sequenceName);


	String getColumnDefaultFromSequence(String sequenceName);

	String createTableWithColumns(TableDef tableDef);

	String sqlType(Class<?> javaType);

	String sqlTypeForJSon();

	Object dropTable(String tablename);

	Object alterColumn(String name, ColumnDef col);

	boolean supportsIdentityStrategy();
}
