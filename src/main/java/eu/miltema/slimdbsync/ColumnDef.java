package eu.miltema.slimdbsync;

import java.lang.reflect.Field;

import javax.persistence.*;
import eu.miltema.slimorm.*;

public class ColumnDef {
	public String name;
	public String type;
	public boolean isNullable;
	public boolean isJson;
	public boolean isPrimaryKey;//only initialized for model columns; not initialized for database columns
	public boolean isIdentity;
	public boolean isIdTableStrategy;

	/**
	 * Value for this column is fetched from this sequence
	 */
	public String sourceSequence;

	/**
	 * A value from @Column(columnDefinition)
	 */
	public String columnDefinitionOverride;

	public ColumnDef() {
	}

	public ColumnDef(EntityProperties eprop, FieldProperties fprop, DatabaseAdapter dbAdapter) {
		name = fprop.columnName;
		isNullable = (fprop == eprop.idField ? false : isNullable(fprop.field));
		isJson = fprop.field.isAnnotationPresent(JSon.class);
		isPrimaryKey = (fprop == eprop.idField);
		type = (isJson ? dbAdapter.sqlTypeForJSon() : dbAdapter.sqlType(fprop.fieldType));
		sourceSequence = getSourceSequence(eprop, fprop, dbAdapter);
		columnDefinitionOverride = (fprop.field.isAnnotationPresent(Column.class) ? fprop.field.getAnnotation(Column.class).columnDefinition() : null);
		if ("".equals(columnDefinitionOverride))
			columnDefinitionOverride = null;
		handleGeneratedValue(fprop.field.getAnnotation(GeneratedValue.class));

		if (isIdentity && !dbAdapter.supportsIdentityStrategy())
			throw new SchemaUpdateException(fprop.field, "identity strategy not supported");
		if (isIdTableStrategy)
			throw new SchemaUpdateException(fprop.field, "table strategy not supported");
	}

	private void handleGeneratedValue(GeneratedValue gv) {
		if (gv != null)
			switch (gv.strategy()) {
			case IDENTITY:
				isIdentity = true;
				break;
			case TABLE:
				isIdTableStrategy = true;
				break;
			default:
				break;
			}
	}

	private boolean isNullable(Field field) {
		if (field.isAnnotationPresent(Id.class))
			return false;
		if (field.isAnnotationPresent(Column.class))
			return field.getAnnotation(Column.class).nullable();
		if (field.isAnnotationPresent(ManyToOne.class))
			return field.getAnnotation(ManyToOne.class).optional();
		Class<?> type = field.getType();
		return !(type == boolean.class || type == short.class || type == int.class || type == long.class || type == double.class || type == float.class);
	}

	private String getSourceSequence(EntityProperties e, FieldProperties f, DatabaseAdapter dbAdapter) {
		if (f.field.isAnnotationPresent(GeneratedValue.class)) {
			GeneratedValue gv = f.field.getAnnotation(GeneratedValue.class);
			if (gv.strategy() == GenerationType.AUTO || gv.strategy() == GenerationType.SEQUENCE) {
				String seqName = gv.generator();
				if (seqName == null || seqName.trim().isEmpty())
					return dbAdapter.getDefaultSequenceName(e.tableName, f.columnName);
				else return seqName.trim();
			}
		}
		else if (isPrimaryKey && !f.field.isAnnotationPresent(Id.class))// this is an id-field without @Id and @GeneratedValue
			return dbAdapter.getDefaultSequenceName(e.tableName, f.columnName);
		return null;
	}
}
