package eu.miltema.slimdbsync;

import java.util.*;

import eu.miltema.slimorm.Database;

public class SyncContext {
	public Database db;
	public String schema;
	public Map<String, TableDef> modelTables; // name->TableDef
	public Map<String, TableDef> dbTables; // name->TableDef
	public Set<String> modelSequenceNames;
	public Set<String> dbSequenceNames;
	public Map<String, PrimaryKeyDef> modelPrimaryKeys; // tbl->PrimaryKeyDef
	public Map<String, PrimaryKeyDef> dbPrimaryKeys; // tbl->PrimaryKeyDef
	public Map<String, ForeignKeyDef> modelForeignKeys; //tbl/col->PrimaryKeyDef
	public Map<String, ForeignKeyDef> dbForeignKeys; //tbl/col->PrimaryKeyDef
	public Map<String, UniqueDef> modelUniques; //tbl/colList->UniqueDef
	public Map<String, UniqueDef> dbUniques; //tbl/colList->UniqueDef

	public String getSchema() {
		return db.getSchema();
	}
}
