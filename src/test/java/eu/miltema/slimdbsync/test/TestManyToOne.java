package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.*;

import eu.miltema.slimdbsync.SchemaGenerator;

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
	public void testNewFKey() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		Entity1 e1 = db.insert(new Entity1("John"));
		EntityFKey ef = new EntityFKey("Ann", e1);
		db.insert(ef);
		assertEquals(1, db.sql("SELECT id, entity1_id FROM entity_fkey").list(EntityFKey.class).size());
	}

	@Test(expected = SQLException.class)
	public void testConstraint() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		Entity1 e1 = new Entity1();
		e1.id = 99999;//refers to non-existing entity
		EntityFKey ef = new EntityFKey("Ann", e1);
		db.insert(ef);
	}

	@Test(expected = SQLException.class)
	public void testAddAnotherFKey() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKeyNoConstraint.class);
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		Entity1 e = new Entity1();
		e.id = 99999;//invalid id
		db.insert(new EntityFKey("John", e));//added constraint must catch invalid id
	}

	@Test
	public void testDropConstraint() throws Exception {
		new SchemaGenerator(db).sync(Entity1.class, EntityFKey.class);
		new SchemaGenerator(db).sync(Entity1.class, EntityFKeyNoConstraint.class);
		Entity1 e = new Entity1();
		e.id = 99999;//invalid id
		EntityFKey ef = db.insert(new EntityFKey("John", e));
		assertEquals(99999, db.getById(EntityFKeyNoConstraint.class, ef.id).entity1Id.intValue());
	}
}
