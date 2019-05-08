package eu.miltema.slimdbsync.def;

import java.util.*;

public class TableDef {

	public String name;
	public Map<String, ColumnDef> columns;
	public List<String> columnOrder = new ArrayList<String>();
}
