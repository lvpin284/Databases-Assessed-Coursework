import java.util.Date;

public class BackupMetadata {
    private String backupFileName;
    private long backupFileSize;
    private Date backupTime;
    private String backupType; // Full, Incremental, Differential

    public BackupMetadata(String backupFileName, long backupFileSize, Date backupTime, String backupType) {
        this.backupFileName = backupFileName;
        this.backupFileSize = backupFileSize;
        this.backupTime = backupTime;
        this.backupType = backupType;
    }

    // Getters and Setters
    public String getBackupFileName() {
        return backupFileName;
    }

    public void setBackupFileName(String backupFileName) {
        this.backupFileName = backupFileName;
    }

    public long getBackupFileSize() {
        return backupFileSize;
    }

    public void setBackupFileSize(long backupFileSize) {
        this.backupFileSize = backupFileSize;
    }

    public Date getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(Date backupTime) {
        this.backupTime = backupTime;
    }

    public String getBackupType() {
        return backupType;
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }
}
