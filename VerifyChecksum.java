import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VerifyChecksum {

    public VerifyChecksum(String filePath, String checksumFilePath) {
        try {
            // Generate checksum of the backup file
            String currentChecksum = generateChecksum(filePath);
            // Read the saved checksum
            String savedChecksum = new String(Files.readAllBytes(Paths.get(checksumFilePath)));
            // Compare the checksums
            if (currentChecksum.equals(savedChecksum)) {
                System.out.println("Checksum verification passed. The backup file is intact.");
            } else {
                System.out.println("Checksum verification failed. The backup file may have been corrupted.");
            }
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
}
