package eu.miltema.slimdbsync.test;

import static org.junit.Assert.assertEquals;

import eu.miltema.slimdbsync.SchemaGenerator;
import eu.miltema.slimorm.Database;

public class SchemaGenEx extends SchemaGenerator {
	private int expectedStatementCount;

	public SchemaGenEx(Database db, int expectedStatementCount) {
		super(db);
		this.expectedStatementCount = expectedStatementCount;
	}

	@Override
	protected void applyChanges(String ddlChanges) throws Exception {
		assertEquals(expectedStatementCount, ddlChanges.split(";").length - 1);
		super.applyChanges(ddlChanges);
	}

}
