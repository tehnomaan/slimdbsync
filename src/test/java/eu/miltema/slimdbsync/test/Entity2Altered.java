package eu.miltema.slimdbsync.test;

import javax.persistence.*;

@Table(name = "entity2")
public class Entity2Altered {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq3")
	public Integer id;

	@Column(nullable = false)
	public String name;

	public Short count2;
}
