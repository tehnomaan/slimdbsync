package eu.miltema.slimdbsync.test;

import javax.persistence.*;

import eu.miltema.slimdbsync.*;

@Indexes({@Index("id"), @Index({"name", "count2"})})
@Table(name = "entity2")
public class Entity2Index {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;
	public String name;
	public Integer count2;

}
