package eu.miltema.slimdbsync.test;

import static org.junit.Assert.*;

import org.junit.*;

import eu.miltema.slimdbsync.DatabaseSync;

public class TestBasics extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testAddTable() throws Exception {
		new DatabaseSync(db).dropUnusedElements(false).sync(SyncEntity6.class);
		db.insert(new SyncEntity6());
		assertTrue(db.listAll(SyncEntity6.class).size() == 1);
	}

	@Test
	public void testAddSequence() throws Exception {
		new DatabaseSync(db).dropUnusedElements(false).sync(SyncEntity1.class);
		SyncEntity1 e = db.insert(new SyncEntity1());
		assertTrue(e.id > 0);
		assertNotNull(db.getById(SyncEntity1.class, e.id));
	}

	@Test
	public void testNoChanges() throws Exception {
		new DatabaseSync(db).sync(SyncTypesEntity.class);
		new DatabaseSync(db) {
			@Override
			protected void applyChanges(String ddlChanges) throws Exception {
				assertTrue(ddlChanges.isEmpty());
			}
		}.sync(SyncTypesEntity.class);
	}
}
