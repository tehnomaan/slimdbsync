package eu.miltema.slimdbsync;

import java.util.*;

import eu.miltema.slimorm.Database;

public class SyncContext {
	public Database db;
	public String schema;
	public Map<String, TableDef> modelTables;//name->TableDef
	public Map<String, TableDef> dbTables;//name->TableDef
	public Set<String> modelSequenceNames;
	public Set<String> dbSequenceNames;
	public Collection<PrimaryKeyDef> dbPrimaryKeys;

	public String getSchema() {
		return db.getSchema();
	}
}
