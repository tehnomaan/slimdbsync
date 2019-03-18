package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import org.junit.*;

import eu.miltema.slimdbsync.*;

public class TestPrimaryKey extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Test
	public void testNoPrimaryKey() throws Exception {
		new DatabaseSync(db).sync(SyncEntity0.class);
		db.insert(new SyncEntity0());
		assertNull(db.listAll(SyncEntity0.class).get(0).uu);
	}

	@Test
	public void testDefaultSequence() throws Exception {
		new DatabaseSync(db).sync(SyncEntity1.class);
		assertTrue(db.insert(new SyncEntity1()).id > 0);
	}

	@Test
	public void testCustomSequence() throws Exception {
		new DatabaseSync(db).sync(SyncEntity2.class);
		assertTrue(db.insert(new SyncEntity2()).id > 0);
	}

	@Test(expected = SchemaUpdateException.class)
	public void testIdentityStrategy() throws Exception {
		new DatabaseSync(db).sync(SyncEntity3.class);
	}

	@Test(expected = SchemaUpdateException.class)
	public void testTableStrategy() throws Exception {
		new DatabaseSync(db).sync(SyncEntity4.class);
	}

	@Test
	public void testAssumedId() throws Exception {
		new DatabaseSync(db).sync(SyncEntity5.class);
		assertTrue(db.insert(new SyncEntity5()).id > 0);
	}

	@Test
	public void testManualId() throws Exception {
		new DatabaseSync(db).sync(SyncEntity6.class);
		long id = System.currentTimeMillis();
		SyncEntity6 e = new SyncEntity6();
		e.id = id;
		e.name = "Mike";
		assertEquals(id, db.insert(e).id);
		assertEquals("Mike", db.getById(SyncEntity6.class, id).name);
	}
}
