package eu.miltema.slimdbsync;

import java.util.*;

import eu.miltema.slimdbsync.def.*;
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
	public Map<String, CheckDef> modelChecks; //tbl/col/valList->CheckDef
	public Map<String, CheckDef> dbChecks; //tbl/col/valList->CheckDef
	public Map<String, IndexDef> modelIndexes; //tbl/colList->IndexDef
	public Map<String, IndexDef> dbIndexes; //tbl/colList->IndexDef

	public String getSchema() {
		return db.getSchema();
	}
}
