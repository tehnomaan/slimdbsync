package eu.miltema.slimdbsync;

import java.lang.reflect.Field;

public class SchemaUpdateException extends RuntimeException {

	public SchemaUpdateException(String message) {
		super(message);
	}

	public SchemaUpdateException(Exception cause) {
		super(cause);
	}

	public SchemaUpdateException(Field field, String message) {
		super(ref(field) + message);
	}

	private static String ref(Field field) {
		return field.getDeclaringClass().getSimpleName() + "." + field.getName() + ": ";
	}
}
