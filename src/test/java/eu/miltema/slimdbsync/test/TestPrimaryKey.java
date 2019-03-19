package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import java.sql.SQLException;

import org.junit.*;

import eu.miltema.slimdbsync.*;

public class TestPrimaryKey extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testNoPrimaryKey() throws Exception {
		new DatabaseSync(db).sync(Entity0.class);
		db.insert(new Entity0());
		assertNull(db.listAll(Entity0.class).get(0).name);
	}

	@Test
	public void testDefaultSequence() throws Exception {
		new DatabaseSync(db).sync(Entity1.class);
		assertTrue(db.insert(new Entity1()).id > 0);
	}

	@Test
	public void testCustomSequence() throws Exception {
		new DatabaseSync(db).sync(Entity2.class);
		assertTrue(db.insert(new Entity2()).id > 0);
	}

	@Test(expected = SchemaUpdateException.class)
	public void testIdentityStrategy() throws Exception {
		new DatabaseSync(db).sync(Entity3.class);
	}

	@Test(expected = SchemaUpdateException.class)
	public void testTableStrategy() throws Exception {
		new DatabaseSync(db).sync(Entity4.class);
	}

	@Test
	public void testAssumedId() throws Exception {
		new DatabaseSync(db).sync(Entity5.class);
		assertTrue(db.insert(new Entity5()).id > 0);
	}

	@Test
	public void testManualId() throws Exception {
		new DatabaseSync(db).sync(Entity6.class);
		long id = System.currentTimeMillis();
		Entity6 e = new Entity6();
		e.id = id;
		e.name = "Mike";
		assertEquals(id, db.insert(e).id);
		assertEquals("Mike", db.getById(Entity6.class, id).name);
	}

	@Test(expected = SQLException.class)
	public void testPrimaryKeyConstraint() throws Exception {
		new DatabaseSync(db).sync(Entity6.class);
		long id = System.currentTimeMillis();
		Entity6 e = new Entity6();
		e.id = id;
		e.name = "Mike";
		db.insert(e);
		db.insert(e);
	}
}
