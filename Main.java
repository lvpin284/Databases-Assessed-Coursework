import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;

public class Main {

DbUser myDbUser = null;


private void go() throws SQLException, IOException {
	String name = null;
	System.out.println("In go...");

	BufferedReader brin = new BufferedReader(new InputStreamReader(System.in));
	System.out.print("type in database name: ");
	try {
		name = brin.readLine();
	}
	catch (IOException e) {
		System.out.println("Main.go() : Failure in I/O"+e);
	};

	myDbUser = new DbUser(name);

	myDbUser.checkBack();
	myDbUser.getData();
	myDbUser.process();

	String filePath = "./backup.sql";
	String checksumFilePath = filePath + ".checksum";
	BackupWithChecksum BackupWithChecksum = new BackupWithChecksum(filePath, checksumFilePath);

	System.out.println("Processing over");

	myDbUser.close();
}; // end of method "go"

public static void main(String [ ] args) throws SQLException, IOException {
	Main myMain = new Main();
	myMain.go();
} // end of method "main"

} // end of class "Main"
