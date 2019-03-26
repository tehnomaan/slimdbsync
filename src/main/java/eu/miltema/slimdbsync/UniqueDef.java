package eu.miltema.slimdbsync;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UniqueDef {
	public String name;//constraint name
	public String tableName;
	public String[] columns;

	@Override
	public boolean equals(Object object) {
		if (object == null || !(object instanceof UniqueDef))
			return false;
		UniqueDef udef = (UniqueDef) object;
		return tableName.equals(udef.tableName) && Arrays.equals(columns, udef.columns);
	}

	@Override
	public String toString() {
		return name + "/" + Arrays.stream(columns).collect(Collectors.joining(","));
	}
}
