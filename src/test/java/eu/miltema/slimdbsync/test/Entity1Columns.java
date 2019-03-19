package eu.miltema.slimdbsync.test;

import javax.persistence.Table;

/**
 * Entity similar to Entity1, but has 1 less field and 1 more field
 */
@Table(name = "entity1")
public class Entity1Columns {
	int id;
	int count2;
}
