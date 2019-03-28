package eu.miltema.slimdbsync.test;

import javax.persistence.*;

@Table(name = "entity2")
public class Entity2Enum {

	enum Name {John, Jack, Joe};

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq2")
	public Integer id;
	public Name name;
	public Integer count2;

}
