package eu.miltema.slimdbsync.test;

import javax.persistence.*;

@Table(name = "entity2")
public class Entity2UniqueField {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;

	@Column(unique = true)
	public String name;

	public Integer count2;

	public Entity2UniqueField() {
	}

	public Entity2UniqueField(String name, int count) {
		this.name = name;
		this.count2 = count;
	}
}
