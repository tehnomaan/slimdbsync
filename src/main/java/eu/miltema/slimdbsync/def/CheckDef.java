package eu.miltema.slimdbsync.def;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Check constraint properties
 * @author Margus
 *
 */
public class CheckDef {
	public String name;//constraint name
	public String tableName;
	public String columnName;
	public String[] validValues;

	@Override
	public String toString() {
		return tableName + "/" + columnName + "/" + Arrays.stream(validValues).collect(Collectors.joining(","));
	}

	public CheckDef(String name, String tableName, String columnName) {
		this.name = name;
		this.tableName = tableName;
		this.columnName = columnName;
	}
}
