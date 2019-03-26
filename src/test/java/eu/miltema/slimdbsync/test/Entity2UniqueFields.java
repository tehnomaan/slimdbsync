package eu.miltema.slimdbsync.test;

import javax.persistence.*;

@Table(name = "entity2", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "count2"}))
public class Entity2UniqueFields {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;

	public String name;

	public Integer count2;

	public Entity2UniqueFields() {
	}

	public Entity2UniqueFields(String name, int count) {
		this.name = name;
		this.count2 = count;
	}
}
