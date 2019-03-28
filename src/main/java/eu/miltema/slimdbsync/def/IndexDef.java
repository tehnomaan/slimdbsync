package eu.miltema.slimdbsync.def;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IndexDef {
	public String name;//index name
	public String tableName;
	public String[] columns;
	public boolean isUniqueIndex;

	@Override
	public String toString() {
		return tableName + "/" + Arrays.stream(columns).collect(Collectors.joining(","));
	}
}
