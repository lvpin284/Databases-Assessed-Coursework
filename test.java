import java.io.IOException;
import java.sql.SQLException;

public class test  {
    public static void main(String [ ] args) throws SQLException, IOException {
        String filePath = "./backup.sql";
        String checksumFilePath = filePath + ".checksum";
        VerifyChecksum VerifyChecksum=new VerifyChecksum(filePath, checksumFilePath);

    } // end of method "main"

}