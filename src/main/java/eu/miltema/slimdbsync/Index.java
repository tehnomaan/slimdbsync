package eu.miltema.slimdbsync;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.*;

@Retention(RUNTIME)
@Target({})
public @interface Index {
	String[] value();
}
