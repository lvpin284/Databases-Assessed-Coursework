// STUBBED FILE

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

// this is the class through which all Database calls go
public class DbUser extends DbBasic {

	private ResultSet rs = null;
	String outputFile = "backup.sql";
	private DatabaseMetaData metaData;
	private String dbName;
	private DbBasic dbBackup;


	/*
	 * Creates a connection to the named database
	 */
	DbUser ( String dbName ) {
		super( dbName );
		this.dbName = dbName;

	}

	/**
	 * Checks for the existence of specified backup files and deletes them if they exist.
	 * Logs the status of each file checked.
	 *
	 * The method checks for the existence of the files "backup.sql" and "backup.db".
	 * If a file exists, it attempts to delete it and logs the result.
	 * If a file does not exist, it logs that the file was not found.
	 *
	 * This method uses the java.util.logging.Logger for logging.
	 */
	public void checkBack(){
		Logger logger = Logger.getLogger("BackupChecker");
		String[] BACKUP_FILES = {"backup.sql", "backup.db"};
		logger.info("Checking for the existence of backup files...");
		for (String fileName : BACKUP_FILES) {
			File file = new File(fileName);
			if (file.exists()) {
				if (file.delete()) {
					System.out.println(fileName + " has been deleted.");
				}
			} else {
				System.out.println(fileName + " does not exist.");
			}
		}
	}


	/**
	 * Retrieves and displays information about the connected database.
	 *
	 * This method fetches the database metadata from the current connection
	 * and prints the database product name, version, and the database file path.
	 *
	 * @throws SQLException if a database access error occurs
	 */
	public void getData( ) throws SQLException, IOException {
		this.metaData = con.getMetaData();
		System.out.println("-----------------Database Info-----------------");
		System.out.println("Database Product: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
		System.out.println("Database Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion());
		System.out.println("Database file path: " + dbName);
		System.out.println("-----------------------------------------------");

		savaData("--Database Product: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion() + "\n", outputFile);
		savaData("--Database Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion() + "\n", outputFile);
		savaData("--Database file path: " + dbName + "\n", outputFile);
	}

	/**
	 * Processes the database backup by creating a new backup file,
	 * generating tables, indexes, views, and insert statements,
	 * and then saving these to the backup database.
	 *
	 * @throws IOException if an I/O error occurs while creating the backup file
	 * @throws SQLException if a database access error occurs
	 */
	public void process() throws IOException, SQLException {
		// Create a new backup file
		new File("backup.db").createNewFile();
		dbBackup = new DbBasic("backup.db");

		// Retrieve table names and initialize index information storage
		ArrayList<String> tables = getOrderedTable();
		ArrayList<String> indexes = new ArrayList<>();

		System.out.println("Creating tables...");
		for (String table : tables) {
			backupTable(table); // Create table in backup
			indexBackup(table, indexes); // Gather index information for table
		}

		// Save index information to the backup SQL file
		if (!indexes.isEmpty()) {
			indexes.set(indexes.size() - 1, indexes.get(indexes.size() - 1) + ");\n");
		}
		for (String index : indexes) {
			savaData(index, outputFile); // Save index SQL to file
			dbBackup.con.createStatement().executeUpdate(index); // Execute index SQL in backup database
		}

		savaData("\n\n", outputFile); // Add spacing in the output file

		System.out.println("Creating insert...");
		// Create and save insert statements for each table
		for (String table : tables) {
			InsertStatement(table);
		}

		System.out.println("Creating views...");
		// Create views in the backup database
		viewBackup();
		// Backup views by creating CREATE VIEW statements in the SQL file
		viewdataBackup();

		// Commit and close the backup database connection
		dbBackup.con.commit();
		dbBackup.con.close();
	}


	/**
	 * Retrieves the list of tables ordered by depth and returns it.
	 *
	 * This method gathers table and foreign key information,
	 * calculates the depth of each table in the dependency hierarchy,
	 * sorts the tables based on their depth, and returns the list of tables.
	 *
	 * @return An ArrayList of table names ordered by depth
	 * @throws SQLException if a database access error occurs
	 */
	private ArrayList<String> getOrderedTable() throws SQLException {
		// Map to store table depths
		Map<String, Integer> depth = new HashMap<>();

		// ArrayLists to store table names and their foreign key references
		ArrayList<String> tables = new ArrayList<>();
		ArrayList<ArrayList<String>> tableRef = new ArrayList<>();

		// Retrieve table information and foreign key references
		getTableAndForeignKeyInfo(tables, tableRef, depth);

		// Sort tables by depth
		sortTablesByDepth(tables, tableRef, depth);

		// Return ordered list of table names
		return new ArrayList<>(depth.keySet());
	}

	/**
	 * Retrieves information about tables and their foreign key references.
	 *
	 * This method queries the database metadata to retrieve information
	 * about tables and views, including their names and foreign key references.
	 * It populates the provided ArrayLists with table names, foreign key references,
	 * and calculates the depth of each table in the dependency hierarchy.
	 *
	 * @param tables An ArrayList to store the names of tables and views
	 * @param tableRef An ArrayList to store the foreign key references for each table
	 * @param depth A Map to store the depth of each table in the dependency hierarchy
	 * @throws SQLException if a database access error occurs
	 */
	private void getTableAndForeignKeyInfo(ArrayList<String> tables, ArrayList<ArrayList<String>> tableRef, Map<String, Integer> depth) throws SQLException {
		// Retrieve all tables and views from the database metadata
		rs = metaData.getTables(null, null, null, new String[]{"TABLE", "VIEW"});

		// Iterate over each table and view
		while (rs.next()) {
			// Get the name of the current table
			String TABLE_NAME = rs.getString("TABLE_NAME");

			// Initialize the depth of the current table to 0
			depth.put(TABLE_NAME, 0);

			// Add the table name to the tables list
			tables.add(TABLE_NAME);

			// Create a list to store foreign key references for the current table
			ArrayList<String> references = new ArrayList<>();

			// Get all foreign key references for the current table
			ResultSet fks = metaData.getImportedKeys(null, null, TABLE_NAME);

			// Iterate over each foreign key reference
			while (fks.next()) {
				// Get the name of the referenced table
				String referencedTable = fks.getString("PKTABLE_NAME");
				// If the referenced table is not the same as the current table, add it to the references list
				if (!referencedTable.equals(TABLE_NAME)) {
					references.add(referencedTable);
				}
			}

			// Add the references list for the current table to the tableRef list
			tableRef.add(references);
		}

		// Close the ResultSet to release resources
		rs.close();
	}

	/**
	 * Sorts the tables by their depth in the dependency hierarchy.
	 *
	 * This method performs a depth-first search (DFS) on each table
	 * to calculate its depth in the dependency hierarchy. It then sorts
	 * the tables based on their depth and updates the provided depth map accordingly.
	 *
	 * @param tables An ArrayList containing the names of tables and views
	 * @param tableRef An ArrayList containing the foreign key references for each table
	 * @param depth A Map containing the depth of each table in the dependency hierarchy
	 */
	private void sortTablesByDepth(ArrayList<String> tables, ArrayList<ArrayList<String>> tableRef, Map<String, Integer> depth) {
		// Perform depth-first search (DFS) on each table
		for (int i = 0; i < tableRef.size(); i++) {
			dfsSort(i, tables, tableRef, depth, 0);
		}

		// Store depth information in a list and sort it by depth
		ArrayList<Map.Entry<String, Integer>> list = new ArrayList<>(depth.entrySet());
		list.sort((o1, o2) -> -o1.getValue().compareTo(o2.getValue()));

		// Clear the original depth map and repopulate it in the sorted order
		depth.clear();
		for(Map.Entry<String, Integer> entry: list){
			depth.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Performs depth-first search (DFS) to calculate the depth of each table in the dependency hierarchy.
	 *
	 * This method recursively traverses the foreign key references of a table to calculate its depth
	 * in the dependency hierarchy. It updates the provided depth map with the maximum depth of each table.
	 *
	 * @param i The index of the current table in the tables list
	 * @param tables An ArrayList containing the names of tables and views
	 * @param tableRef An ArrayList containing the foreign key references for each table
	 * @param depth A Map containing the depth of each table in the dependency hierarchy
	 * @param d The current depth in the DFS traversal
	 */
	private void dfsSort(int i, ArrayList<String> tables, ArrayList<ArrayList<String>> tableRef, Map<String, Integer> depth, int d) {
		// Update the depth of the current table to the maximum of the current depth and the existing depth
		depth.put(tables.get(i), Math.max(d, depth.get(tables.get(i))));

		// If there are no foreign key references for the current table, return
		if (tableRef.get(i).isEmpty()) return;

		// Perform depth-first search (DFS) on each foreign key reference of the current table
		for (String referencedTable : tableRef.get(i)) {
			// Get the index of the referenced table in the tables list and continue DFS traversal
			dfsSort(tables.indexOf(referencedTable), tables, tableRef, depth, d + 1);
		}
	}


	/**
	 * Backs up the specified table by creating its SQL representation.
	 *
	 * This method checks if the specified object is a table (not a view),
	 * constructs the SQL CREATE TABLE statement for the table including columns,
	 * primary keys, and foreign keys, and executes it to create the table in the backup database.
	 *
	 * @param table The name of the table to be backed up
	 * @throws SQLException if a database access error occurs
	 * @throws IOException if an I/O error occurs while writing to the output file
	 */
	private void backupTable(String table) throws SQLException, IOException {
		// Check if the object is a table (not a view)
		rs = metaData.getTables(null, null, table, new String[]{"TABLE"});
		if (rs.next()) {
			// Build the SQL CREATE TABLE statement
			StringBuilder createSQLStatement = new StringBuilder();
			createSQLStatement.append("CREATE TABLE \"").append(table).append("\" (\n");
			// Append column information to the SQL statement
			addColumns(createSQLStatement, table);
			// Append primary key information to the SQL statement
			addPreKey(createSQLStatement, table);
			// Append foreign key information to the SQL statement
			addForKey(createSQLStatement, table);
			// Complete the SQL statement
			createSQLStatement.append("\n);\n");

			// If the table already exists, drop it and then create it again
			String DROP = "DROP TABLE IF EXISTS \"" + table + "\";\n";

			savaData(DROP, outputFile);
			dbBackup.con.createStatement().executeUpdate(DROP);

			// Check if the table already exists in the backup database
			if (!tableExists(table)) {
				// If the table does not exist, save the SQL statement to the output file and execute it
				savaData(createSQLStatement.toString(), outputFile);
				dbBackup.con.createStatement().executeUpdate(createSQLStatement.toString());
			} else {
				System.out.println("Table " + table + " already exists");
				dbBackup.con.createStatement().executeUpdate(DROP);
				savaData(createSQLStatement.toString(), outputFile);
				dbBackup.con.createStatement().executeUpdate(createSQLStatement.toString());
			}
		}
		// Close the ResultSet to release resources
		rs.close();
	}

	/**
	 * Checks if the specified table exists in the backup database.
	 *
	 * This method queries the database metadata to determine if the specified table exists
	 * in the backup database.
	 *
	 * @param table The name of the table to check for existence
	 * @return true if the table exists in the backup database, false otherwise
	 * @throws SQLException if a database access error occurs
	 */
	private boolean tableExists(String table) throws SQLException {
		rs = dbBackup.con.getMetaData().getTables(null, null, table, null);
		return rs.next();
	}

	/**
	 * Appends column definitions to the provided SQL statement for the specified table.
	 *
	 * This method retrieves all column information for the specified table from the database metadata,
	 * and appends the column definitions (including column name, type, and nullability) to the provided
	 * SQL statement.
	 *
	 * @param statement The StringBuilder to which the column definitions will be appended
	 * @param table The name of the table for which the column definitions are being retrieved
	 * @throws SQLException if a database access error occurs
	 */
	private void addColumns(StringBuilder statement, String table) throws SQLException {
		// Retrieve all column information for the specified table
		ResultSet colRS = metaData.getColumns(null, null, table, null);

		// Flag to indicate if this is the first column
		boolean isFirstCol = true;

		// Iterate over each column
		while (colRS.next()) {
			// Get column name, type, and nullability information
			String colName = colRS.getString("COLUMN_NAME");
			String colType = colRS.getString("TYPE_NAME");
			String nullLable = colRS.getString("NULLABLE");
			String defaultLable = colRS.getString("COLUMN_DEF");

			// Append column information to the SQL statement
			statement.append(isFirstCol ? "\t" : ",\n\t").append(colName).append(" ").append(colType);

			// If the column is not nullable, append "not null"
			if ("0".equals(nullLable)) {
				statement.append(" not null");
			}
			// If the column has a default value, append "default" followed by the default value
			if (defaultLable != null) {
				statement.append(" default ").append(defaultLable);
			}

			// Mark that the first column has been processed
			isFirstCol = false;
		}
		// Close the ResultSet to release resources
		colRS.close();
	}

	/**
	 * Appends primary key definitions to the provided SQL statement for the specified table.
	 *
	 * This method retrieves all primary key information for the specified table from the database metadata,
	 * and appends the primary key definitions to the provided SQL statement.
	 *
	 * @param statement The StringBuilder to which the primary key definitions will be appended
	 * @param table The name of the table for which the primary key definitions are being retrieved
	 * @throws SQLException if a database access error occurs
	 */
	private void addPreKey(StringBuilder statement, String table) throws SQLException {
		// Retrieve primary key information for the specified table
		ResultSet keyRS = metaData.getPrimaryKeys(null, null, table);

		// Create a list to store the primary key column names
		ArrayList<String> priKey = new ArrayList<>();

		// Iterate over the primary key result set and add the column names to the list
		while (keyRS.next()) {
			priKey.add(keyRS.getString("COLUMN_NAME"));
		}

		// Close the ResultSet to release resources
		keyRS.close();

		// If there are primary key columns, append them to the SQL statement
		if (!priKey.isEmpty()) {
			statement.append(",\n\tPRIMARY KEY (").append(String.join(", ", priKey)).append(")");
		}
	}

	/**
	 * Appends foreign key definitions to the provided SQL statement for the specified table.
	 *
	 * This method retrieves all foreign key information for the specified table from the database metadata,
	 * and appends the foreign key definitions to the provided SQL statement.
	 *
	 * @param statement The StringBuilder to which the foreign key definitions will be appended
	 * @param table The name of the table for which the foreign key definitions are being retrieved
	 * @throws SQLException if a database access error occurs
	 */
	private void addForKey(StringBuilder statement, String table) throws SQLException {
		// Retrieve foreign key information for the specified table
		ResultSet forKeyRS = metaData.getImportedKeys(null, null, table);

		// Flag to indicate if this is the first foreign key
		boolean isFirstCol = true;

		// Iterate over the foreign key result set
		while (forKeyRS.next()) {
			// Append foreign key information to the SQL statement
			statement.append(isFirstCol ? ",\n\tFOREIGN KEY (" : ",\n\tFOREIGN KEY (")
					.append(forKeyRS.getString("FKCOLUMN_NAME"))
					.append(") REFERENCES \"")
					.append(forKeyRS.getString("PKTABLE_NAME"))
					.append("\"(")
					.append(forKeyRS.getString("PKCOLUMN_NAME"))
					.append(")")
					.append(Action(" ON DELETE", forKeyRS.getString("DELETE_RULE")))
					.append(Action(" ON UPDATE", forKeyRS.getString("UPDATE_RULE")));

			// Update flag indicating that the first foreign key has been processed
			isFirstCol = false;
		}

		// Close the ResultSet to release resources
		forKeyRS.close();
	}

	/**
	 * Gets the cascade action for the given rule.
	 *
	 * This helper method converts the specified rule to its corresponding cascade action string.
	 *
	 * @param actionType The type of action (e.g., " ON DELETE" or " ON UPDATE")
	 * @param rule The rule to be converted
	 * @return The cascade action string corresponding to the rule
	 */
	private String Action(String actionType, String rule) {
		switch (rule) {
			case "0": // CASCADE
				return actionType + " CASCADE";
			case "1": // RESTRICT
				return actionType + " RESTRICT";
			case "2": // SET NULL
				return actionType + " SET NULL";
			case "3": // NO ACTION
				return actionType + " NO ACTION";
			case "4": // SET DEFAULT
				return actionType + " SET DEFAULT";
			default:
				return "";
		}
	}

	/**
	 * Appends the specified data to a file.
	 *
	 * This method writes the provided data string to the specified file. If the file already exists,
	 * the data is appended to the end of the file.
	 *
	 * @param data The data to be written to the file
	 * @param file The path to the file where the data will be written
	 * @throws IOException if an I/O error occurs
	 */
	private void savaData(String data, String file) throws IOException {
		try (FileWriter fileWriter = new FileWriter(file, true)) {
			fileWriter.write(data);
		}
	}

	/**
	 * Backs up the index information for the specified table and adds the SQL statements to the provided list.
	 *
	 * This method retrieves the index information for the specified table from the database metadata
	 * and constructs the SQL statements needed to recreate those indexes. The SQL statements are added
	 * to the provided list.
	 *
	 * @param table The name of the table for which the index information is being backed up
	 * @param indexes The list to which the SQL statements for the indexes will be added
	 * @throws SQLException if a database access error occurs
	 */
	private void indexBackup(String table, ArrayList<String> indexes) throws SQLException {
		// Retrieve index information for the specified table
		try (ResultSet indexRS = metaData.getIndexInfo(null, null, table, false, true)) {
			// Iterate over the index result set
			while (indexRS.next()) {
				// Get the index name
				String indexName = indexRS.getString("INDEX_NAME");
				// Skip SQLite auto-generated indexes
				if (indexName.contains("sqlite_autoindex_"))
					continue;

				// If the current column is the first column of the index
				if (indexRS.getString("ORDINAL_POSITION").equals("1")) {
					// Complete the previous index statement and add it to the list
					if (!indexes.isEmpty())
						indexes.set(indexes.size() - 1, indexes.get(indexes.size() - 1) + ");\n");

					// Construct the current index statement and add it to the list
					String indexType = indexRS.getString("NON_UNIQUE").equals("1") ? "" : "UNIQUE";
					String colName = indexRS.getString("COLUMN_NAME");
					String order = getOrder(indexRS.getString("ASC_OR_DESC"));

					indexes.add("CREATE " + indexType + " INDEX " + indexName + " ON " + table + " (" + colName + order);
				} else {
					// If the current column is not the first column of the index, append it to the previous index statement
					String colName = indexRS.getString("COLUMN_NAME");
					String order = getOrder(indexRS.getString("ASC_OR_DESC"));
					indexes.set(indexes.size() - 1, indexes.get(indexes.size() - 1) + ", " + colName + order);
				}
			}
		}
	}

	/**
	 * Gets the order string for the given order code.
	 *
	 * This helper method converts the specified order code to its corresponding SQL order string.
	 *
	 * @param orderCode The order code (e.g., "A" for ascending or "D" for descending)
	 * @return The SQL order string corresponding to the order code
	 */
	private String getOrder(String orderCode) {
		if (orderCode == null) return "";
		return switch (orderCode) {
			case "D" -> " DESC";
			case "A" -> " ASC";
			default -> "";
		};
	}

	/**
	 * Backs up all views from the current database to the backup database.
	 *
	 * This method retrieves all views from the current database, checks if they already exist in the backup database,
	 * and if not, retrieves their definitions and recreates them in the backup database.
	 *
	 * @throws SQLException if a database access error occurs
	 * @throws IOException if an I/O error occurs
	 */
	private void viewBackup() throws SQLException, IOException {
		// Retrieve the result set of all views
		try (ResultSet rs = metaData.getTables(null, null, null, new String[]{"VIEW"})) {
			// Iterate over the result set of views
			while (rs.next()) {
				// Get the view name
				String view = rs.getString("TABLE_NAME");

				// Check if the view already exists in the backup database
				if (!viewExists(view)) {
					// Query the definition of the view
					String viewDef = "";
					try (ResultSet viewRS = con.createStatement().executeQuery(
							"SELECT sql FROM sqlite_master WHERE type='view' AND name='" + view + "'")) {
						if (viewRS.next()) {
							viewDef = viewRS.getString("sql");
						}
					}

					// Execute the view definition in the backup database
					dbBackup.con.createStatement().executeUpdate(viewDef);
				} else {
					System.out.println("View " + view + " already exists, skipping creation.");
				}
			}
		}
	}

	/**
	 * Checks if the specified view exists in the backup database.
	 *
	 * @param view The name of the view to check
	 * @return true if the view exists, false otherwise
	 * @throws SQLException if a database access error occurs
	 */
	private boolean viewExists(String view) throws SQLException {
		try (ResultSet rs = dbBackup.con.getMetaData().getTables(null, null, view, new String[]{"VIEW"})) {
			return rs.next();
		}
	}

	/**
	 * Generates and executes INSERT statements for the specified table.
	 *
	 * This method retrieves all data from the specified table, constructs INSERT
	 * statements for each row, and executes those statements on the backup database.
	 * The SQL statements are also saved to the specified output file.
	 *
	 * @param table The name of the table for which INSERT statements are generated
	 * @throws SQLException if a database access error occurs
	 * @throws IOException if an I/O error occurs while saving the statements to a file
	 */
	private void InsertStatement(String table) throws SQLException, IOException {
		// Check if the specified table is a view
		try (ResultSet rs = metaData.getTables(null, null, table, new String[]{"VIEW"})) {
			if (rs.next()) {
				// Skip inserting data for views
				return;
			}
		}

		// Create the SELECT statement to retrieve data from the table
		String selectStatement = "SELECT * FROM \"" + table + "\";";

		// Execute the SELECT statement and retrieve the result set
		try (Statement statement = con.createStatement();
			 ResultSet viewRS = statement.executeQuery(selectStatement)) {

			// Get the metadata of the result set
			ResultSetMetaData resultSetMetaData = viewRS.getMetaData();
			int colCount = resultSetMetaData.getColumnCount();

			// Iterate over the result set and generate INSERT statements for each row
			while (viewRS.next()) {
				// Construct the INSERT statement
				String insertStatement = generateInsertStatement(viewRS, colCount, table);

				// Save the INSERT statement to the output file
				savaData(insertStatement, outputFile);

				// Execute the INSERT statement on the backup database
				dbBackup.con.createStatement().executeUpdate(insertStatement);
			}
		}
	}



	/**
	 * Generates an INSERT statement for the current row of the given ResultSet.
	 * The method handles different data types, including BLOB data.
	 *
	 * @param resultSet The ResultSet containing the data.
	 * @param colCount The number of columns in the ResultSet.
	 * @param table The name of the table.
	 * @return The generated INSERT statement.
	 * @throws SQLException if a database access error occurs.
	 */
	private String generateInsertStatement(ResultSet resultSet, int colCount, String table) throws SQLException {
		StringBuilder insertStatement = new StringBuilder("INSERT INTO \"")
				.append(table).append("\" VALUES (");

		for (int i = 1; i <= colCount; i++) {
			String data;
			// Check if the column type is BLOB
			if (resultSet.getMetaData().getColumnType(i) == java.sql.Types.BLOB) {
				byte[] blob = resultSet.getBytes(i);
				if (blob != null) {
					// Convert the BLOB data to a hexadecimal string
					StringBuilder hexString = new StringBuilder();
					for (byte b : blob) {
						String hex = Integer.toHexString(b & 0xFF);
						if (hex.length() == 1) {
							hexString.append('0');
						}
						hexString.append(hex);
					}
					data = hexString.toString();
				} else {
					data = null;
				}
				data = processData(data);
				insertStatement.append(addDataToStatement(data, i, colCount, false));
			} else {
				data = resultSet.getString(i);
				data = processData(data);
				insertStatement.append(addDataToStatement(data, i, colCount, true));
			}
		}
		insertStatement.append(");\n");
		return insertStatement.toString();
	}

	/**
	 * Processes the data before adding it to the INSERT statement.
	 *
	 * @param data The data to be processed.
	 * @return The processed data.
	 */
	private String processData(String data) {
		if (data == null) {
			return null;
		}
		return data.replaceAll("\"", "'");
	}

	/**
	 * Adds data to the INSERT statement with the correct formatting.
	 * For string types, the data is enclosed in double quotes.
	 * For other types, the data is enclosed in single quotes with a prefix 'X'.
	 *
	 * @param data The data to add to the statement.
	 * @param currentIndex The current index of the column being processed.
	 * @param colCount The total number of columns.
	 * @param isStringType Indicates if the data is a string type.
	 * @return The formatted data to be added to the statement.
	 */
	private String addDataToStatement(String data, int currentIndex, int colCount, boolean isStringType) {
		StringBuilder statement = new StringBuilder();
		// Enclose string types in double quotes; otherwise, enclose in single quotes with a prefix 'X'
		if (isStringType) {
			statement.append("\"").append(data).append("\"");
		} else {
			statement.append("X'").append(data).append("'");
		}
		// Add a comma separator if not the last column
		if (currentIndex != colCount) {
			statement.append(", ");
		}
		return statement.toString();
	}

	/**
	 * Backs up all views by retrieving their creation statements from the database.
	 *
	 * @throws SQLException if a database access error occurs.
	 * @throws IOException  if an I/O error occurs.
	 */
	private void viewdataBackup() throws SQLException, IOException {
		// Retrieve the result set containing all views
		ResultSet viewResultSet = metaData.getTables(null, null, null, new String[]{"VIEW"});
		// Iterate over the views result set
		while (viewResultSet.next()) {
			// Backup the current view
			viewCreator(viewResultSet.getString("TABLE_NAME"));
		}
	}

	/**
	 * Creates a backup of the specified view by retrieving its creation statement from the database.
	 *
	 * @param viewName The name of the view to be backed up.
	 * @throws SQLException if a database access error occurs.
	 * @throws IOException  if an I/O error occurs.
	 */
	private void viewCreator(String viewName) throws SQLException, IOException {
		// Query the database to get the creation statement of the specified view
		ResultSet viewRS = con.createStatement().executeQuery(
				"SELECT sql FROM sqlite_master WHERE type='view' AND name='" + viewName + "'");

		// Get the creation statement of the view
		String viewStatement = viewRS.getString(1);

		// Save the view creation statement to the backup file
		savaData(viewStatement + ";\n", outputFile);
	}
}
