package eu.miltema.slimdbsync.def;

import java.lang.reflect.Field;
import javax.persistence.*;

import eu.miltema.slimdbsync.DatabaseAdapter;
import eu.miltema.slimdbsync.SchemaUpdateException;
import eu.miltema.slimorm.*;

public class ModelColumnDef extends ColumnDef {
	public boolean isPrimaryKey;//only initialized for model columns; not initialized for database columns
	public boolean isIdentity;
	public boolean isUnique;//only initialized for model columns; not initialized for database columns
	public boolean isIdTableStrategy;
	public boolean isForeignKey;

	public ModelColumnDef(EntityProperties eprop, FieldProperties fprop, DatabaseAdapter dbAdapter) {
		name = fprop.columnName;
		isNullable = (fprop == eprop.idField ? false : isNullable(fprop.field));
		isJson = fprop.field.isAnnotationPresent(JSon.class);
		isPrimaryKey = (fprop == eprop.idField);
		if (fprop.field.isAnnotationPresent(ManyToOne.class))
			isForeignKey = true;//cannot set type here, since target entity may have not been initialized yet
		else type = (isJson ? dbAdapter.sqlTypeForJSon() : dbAdapter.sqlType(fprop.fieldType));
		sourceSequence = getSourceSequence(eprop, fprop, dbAdapter);
		if (fprop.field.isAnnotationPresent(Column.class)) {
			Column column = fprop.field.getAnnotation(Column.class);
			columnDefinitionOverride = column.columnDefinition();
			if ("".equals(columnDefinitionOverride))
				columnDefinitionOverride = null;
			if (column.unique())
				isUnique = true;
		}
		initId(fprop.field.getAnnotation(GeneratedValue.class));

		if (isIdentity && isForeignKey)
			throw new SchemaUpdateException(fprop.field, "@Id and @ManyToOne are mutually exclusive");
		if (isIdentity && !dbAdapter.supportsIdentityStrategy())
			throw new SchemaUpdateException(fprop.field, "identity strategy not supported");
		if (isIdTableStrategy)
			throw new SchemaUpdateException(fprop.field, "table strategy not supported");
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

	private void initId(GeneratedValue gv) {
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

	public String getSourceSequence(EntityProperties e, FieldProperties f, DatabaseAdapter dbAdapter) {
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

	@Override
	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}
}
