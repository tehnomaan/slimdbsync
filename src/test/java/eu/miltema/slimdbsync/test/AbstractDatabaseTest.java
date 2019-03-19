package eu.miltema.slimdbsync.test;

import java.io.*;
import java.sql.*;
import java.util.stream.Collectors;

import eu.miltema.slimorm.Database;

abstract class AbstractDatabaseTest {

	protected static Database db;

	protected static final void initDatabase() throws Exception {
		if (db == null) {
			db = new Database("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/slimtest", "slimuser", "slim1234");
			dropAllArtifacts();
		}
	}

	protected static void dropAllArtifacts() throws Exception {
		String sql;
		try(BufferedReader r = new BufferedReader(new InputStreamReader(AbstractDatabaseTest.class.getResourceAsStream("/pg.sql")))) {
			sql = r.lines().collect(Collectors.joining("\r\n"));
		}
		db.transaction((db, connection) -> {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute(sql);
				return 5;
			}
		});
	}

	protected void execSql(String sql) throws Exception {
		db.transaction((db, conn) -> {
			try(PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.executeUpdate();
			}
			return null;
		});
	}
}
