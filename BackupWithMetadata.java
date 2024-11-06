import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class BackupWithMetadata {

    public static void main(String[] args) {
        String filePath = "./backup.sql";
        String metadataFilePath = "./backup_metadata.json";

        // 这里假设备份文件已经生成
        File backupFile = new File(filePath);

        // 创建元数据对象
        BackupMetadata metadata = new BackupMetadata(
                backupFile.getName(),
                backupFile.length(),
                new Date(),
                "Full" // 备份类型可以根据实际情况更改
        );

        try {
            // 确保目录存在
            ensureDirectoryExists(metadataFilePath);
            // 将元数据保存到文件
            saveMetadata(metadataFilePath, metadata);
            System.out.println("Backup and metadata recording completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private static void saveMetadata(String metadataFilePath, BackupMetadata metadata) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(metadataFilePath)) {
            gson.toJson(metadata, writer);
        }
    }
}
