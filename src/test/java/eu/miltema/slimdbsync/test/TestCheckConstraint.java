package eu.miltema.slimdbsync.test;

import java.sql.SQLException;
import org.junit.*;
import eu.miltema.slimdbsync.SchemaGenerator;

public class TestCheckConstraint extends AbstractDatabaseTest {

	@BeforeClass
	public static void setupClass() throws Exception {
		initDatabase();
	}

	@Before
	public void setup() throws Exception {
		dropAllArtifacts();
	}

	@Test
	public void testConstraintOk() throws Exception {
		new SchemaGenerator(db).sync(Entity2Enum.class);
		db.insert(new Entity2("John", 15));
		db.insert(new Entity2("Joe", 16));
		db.insert(new Entity2(null, 17));
	}

	@Test(expected = SQLException.class)
	public void testConstraintFails() throws Exception {
		new SchemaGenerator(db).sync(Entity2Enum.class);
		db.insert(new Entity2("Mary", 15));//exception must be thrown since valid values are John, Jack, Joe
	}

	@Test
	public void testNoChanges() throws Exception {
		new SchemaGenerator(db).sync(Entity2Enum.class);
		new SchemaGenEx(db, 0).sync(Entity2Enum.class);
	}

	@Test(expected = SQLException.class)
	public void testAddConstraint() throws Exception {
		new SchemaGenerator(db).sync(Entity2.class);
		new SchemaGenEx(db, 1).sync(Entity2Enum.class);//add check constraint
		db.insert(new Entity2("Mary", 15));//exception must be thrown since valid values are John, Jack, Joe
	}

	@Test
	public void testDropConstraint() throws Exception {
		new SchemaGenerator(db).sync(Entity2Enum.class);
		new SchemaGenEx(db, 1).sync(Entity2.class);//drop check constraint
		db.insert(new Entity2("Mary", 15));
	}
}
