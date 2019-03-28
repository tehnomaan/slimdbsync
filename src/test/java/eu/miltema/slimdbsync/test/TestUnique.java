package eu.miltema.slimdbsync.test;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;

import org.junit.*;

import eu.miltema.slimdbsync.SchemaGenerator;

public class TestUnique extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testAddSingleFieldUniqueOk() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 1).sync(Entity2UniqueField.class);//only 1 statement: add unique
		db.insert(new Entity2UniqueField("John", 16));
		db.insert(new Entity2UniqueField("Jack", 16));
		assertEquals(2, db.listAll(Entity2UniqueField.class).size());
	}

	@Test(expected = SQLException.class)
	public void testAddSingleFieldUniqueFail() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 1).sync(Entity2UniqueField.class);//only 1 statement: add unique
		db.insert(new Entity2UniqueField("John", 16));
		db.insert(new Entity2UniqueField("John", 23));//should fail
	}

	@Test
	public void testDropSingleFieldUnique() throws Exception {
		new SchemaGenerator(db).sync(Entity2UniqueField.class);
		new SchemaGenEx(db, 1).sync(Entity2.class);//only 1 statement: drop unique
		db.insert(new Entity2UniqueField("John", 16));
		db.insert(new Entity2UniqueField("John", 16));
	}

	@Test
	public void testAddMultiFieldUniqueOk() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 1).sync(Entity2UniqueFields.class);//only 1 statement: add unique
		db.insert(new Entity2UniqueFields("John", 16));
		db.insert(new Entity2UniqueFields("John", 23));
		db.insert(new Entity2UniqueFields("Jack", 16));
		assertEquals(3, db.listAll(Entity2UniqueFields.class).size());
	}

	@Test(expected = SQLException.class)
	public void testAddMultiFieldUniqueFail() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 1).sync(Entity2UniqueFields.class);//only 1 statement: add unique
		db.insert(new Entity2UniqueFields("John", 23));
		db.insert(new Entity2UniqueFields("John", 23));//should fail
	}

	@Test
	public void testDropMultiFieldUnique() throws Exception {
		new SchemaGenerator(db).sync(Entity2UniqueFields.class);
		new SchemaGenEx(db, 1).sync(Entity2.class);//only 1 statement: drop unique
		db.insert(new Entity2UniqueFields("John", 16));
		db.insert(new Entity2UniqueFields("John", 16));
	}

	public void testNoChanges() throws Exception {
		new SchemaGenerator(db).sync(Entity2UniqueField.class);
		new SchemaGenEx(db, 0).sync(Entity2UniqueField.class);
		new SchemaGenerator(db).sync(Entity2UniqueFields.class);
		new SchemaGenEx(db, 0).sync(Entity2UniqueFields.class);
	}
}
