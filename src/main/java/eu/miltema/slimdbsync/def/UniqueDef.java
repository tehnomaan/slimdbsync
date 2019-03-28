package eu.miltema.slimdbsync.def;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UniqueDef {
	public String name;//constraint name
	public String tableName;
	public String[] columns;

	@Override
	public String toString() {
		return tableName + "/" + Arrays.stream(columns).collect(Collectors.joining(","));
	}
}
