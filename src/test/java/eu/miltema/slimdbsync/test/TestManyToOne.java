package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import org.junit.*;

import eu.miltema.slimdbsync.SchemaGenerator;
import eu.miltema.slimorm.TransactionException;

public class TestManyToOne extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testAddFKey() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		Entity1 e1 = db.insert(new Entity1("John"));
		EntityFKey ef = new EntityFKey("Ann", e1);
		db.insert(ef);
		assertEquals(1, db.sql("SELECT id, entity1_id FROM entity_fkey").list(EntityFKey.class).size());
	}

	@Test(expected = TransactionException.class)
	public void testConstraintExists() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		Entity1 e1 = new Entity1();
		e1.id = 99999;//refers to non-existing entity
		EntityFKey ef = new EntityFKey("Ann", e1);
		db.insert(ef);
	}
}
