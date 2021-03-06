package eu.miltema.slimdbsync;

import java.util.*;

import eu.miltema.slimdbsync.def.*;
import eu.miltema.slimorm.Database;

public interface DatabaseAdapter {

	/**
	 * Load current sequences from database
	 * @param db database link
	 * @return set of sequence names
	 * @throws Exception when any error occurs
	 */
	Set<String> loadCurrentSequenceNames(Database db) throws Exception;

	/**
	 * Load current table definitions (incl column definitions) from database
	 * @param db database link
	 * @return table definitions
	 * @throws Exception when any error occurs
	 */
	Collection<TableDef> loadCurrentTables(Database db) throws Exception;

	/**
	 * Load current primary key constraints from database
	 * @param db database link
	 * @return primary key definitions
	 * @throws Exception when any error occurs
	 */
	Collection<PrimaryKeyDef> loadCurrentPrimaryKeys(Database db) throws Exception;

	/**
	 * Load current foreign key constraints from database
	 * @param db database link
	 * @return foreign key definitions
	 * @throws Exception when any error occurs
	 */
	Collection<ForeignKeyDef> loadCurrentForeignKeys(Database db) throws Exception;

	/**
	 * Load current unique constraints from database
	 * @param db database link
	 * @return unique constraint definitions
	 * @throws Exception when any error occurs
	 */
	Collection<UniqueDef> loadCurrentUniques(Database db) throws Exception;

	/**
	 * Load current check constraints from database
	 * @param db database link
	 * @return check constraint definitions
	 * @throws Exception when any error occurs
	 */
	Collection<CheckDef> loadCurrentChecks(Database db) throws Exception;

	/**
	 * Load current indexes from database
	 * @param db database link
	 * @return index definitions
	 * @throws Exception when any error occurs
	 */
	Collection<IndexDef> loadCurrentIndexes(Database db) throws Exception;

	/**
	 * Provide CREATE TABLE statement (including column definitions)
	 * @param tableDef table definition
	 * @return DDL for table creation
	 */
	String createTableWithColumns(TableDef tableDef);

	/**
	 * Provide DROP TABLE statement
	 * @param tablename table name
	 * @return DDL for table deletion
	 */
	String dropTable(String tablename);


	/**
	 * Provide ALTER TABLE statement for adding a column
	 * @param tableName table name
	 * @param column column definition
	 * @return ALTER TABLE ... ADD column statement
	 */
	String addColumn(String tableName, ColumnDef column);

	/**
	 * Provide ALTER TABLE ... DROP COLUMN statement for dropping a column
	 * @param tableName table name
	 * @param columnName column name
	 * @return ALTER TABLE ... DROP column statement
	 */
	String dropColumn(String tableName, String columnName);

	/**
	 * Get default sequence name for database column
	 * @param tablename table name
	 * @param columnName column name
	 * @return sequence name
	 */
	String getDefaultSequenceName(String tablename, String columnName);

	/**
	 * Provide CREATE SEQUENCE statement
	 * @param sequenceName sequence name
	 * @return DDL for sequence creation
	 */
	Object createSequence(String sequenceName);

	/**
	 * Provide DROP SEQUENCE statement
	 * @param sequenceName sequence name
	 * @return DDL for sequence deletion
	 */
	Object dropSequence(String sequenceName);

	/**
	 * Provide ALTER TABLE statement for creating primary key
	 * @param tableName table name
	 * @param columnName column name
	 * @return DDL for creating primary key
	 */
	String addPrimaryKey(String tableName, String columnName);

	/**
	 * Provide statement for dropping primary key 
	 * @param tableName table name
	 * @param columnName column name
	 * @param constraintName constraint name
	 * @return DDL for deleting constraint
	 */
	String dropPrimaryKey(String tableName, String columnName, String constraintName);

	/**
	 * Provide statement for creating foreign key constraint 
	 * @param localTable local table name
	 * @param localColumn local column name
	 * @param constraintName constraint name
	 * @return DDL for dropping foreign key constraint
	 */
	String dropForeignKey(String localTable, String localColumn, String constraintName);

	/**
	 * @param foreignKeyDef foreign key definition
	 * @return DDL for creating foreign key constraint
	 */
	String createForeignKey(ForeignKeyDef foreignKeyDef);

	/**
	 * Map java type to sql type
	 * @param javaType java class
	 * @return sql type
	 */
	String sqlType(Class<?> javaType);

	/**
	 * Get sql type for JSON columns
	 * @return sql type
	 */
	String sqlTypeForJSon();

	/**
	 * @return true, if database supports IDENTITY
	 */
	boolean supportsIdentityStrategy();

	/**
	 * @param tableName table name
	 * @param columnName column name
	 * @param sqlType sql type
	 * @return DDL for altering column type
	 */
	String alterColumnType(String tableName, String columnName, String sqlType);

	/**
	 * @param tableName table name
	 * @param columnName column name
	 * @param isNullable true, if column should become nullable
	 * @return DDL for altering column nullability
	 */
	String alterColumnNullability(String tableName, String columnName, boolean isNullable);

	/**
	 * @param tableName table name
	 * @param columnName column name
	 * @param sourceSequence sequence name for default value
	 * @return DDL for changing column default value
	 */
	String alterColumnDefaultValue(String tableName, String columnName, String sourceSequence);

	/**
	 * @param uniqueDef unique constraint definition
	 * @return DDL for creating unique constraint
	 */
	String createUnique(UniqueDef uniqueDef);

	/**
	 * @param uniqueDef unique constraint definition
	 * @return DDL for dropping unique constraint
	 */
	String dropUnique(UniqueDef uniqueDef);

	/**
	 * @param checkDef check constraint definition
	 * @return DDL for creating check constraint
	 */
	String createCheck(CheckDef checkDef);

	/**
	 * @param checkDef check constraint definition
	 * @return DDL for dropping check constraint
	 */
	String dropCheck(CheckDef checkDef);

	/**
	 * @param indexDef index definition
	 * @return DDL for creating index
	 */
	String createIndex(IndexDef indexDef);

	/**
	 * @param indexDef index definition
	 * @return DDL for dropping index
	 */
	String dropIndex(IndexDef indexDef);
}
