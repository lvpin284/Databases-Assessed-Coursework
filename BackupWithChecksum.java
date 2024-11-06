import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BackupWithChecksum {

    public BackupWithChecksum(String filePath, String checksumFilePath) {
        try {
            // Generate checksum
            String checksum = generateChecksum(filePath);
            // Save checksum to a file
            saveChecksum(checksumFilePath, checksum);
            System.out.println("Backup and checksum generation completed.");
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private String generateChecksum(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void saveChecksum(String checksumFilePath, String checksum) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(checksumFilePath)) {
            fos.write(checksum.getBytes());
        }
    }
}
