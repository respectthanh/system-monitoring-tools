package monitor.ui.service;

import monitor.ui.model.FileSystemInfo;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for file system operations
 * Demonstrates Service Layer pattern and Caching
 */
public class FileSystemService {
    
    private final SystemInfoFactory factory;
    private long lastFileSystemFetch = 0;
    private List<FileSystemInfo> lastFileSystemCache = new ArrayList<>();
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache
    
    public FileSystemService() {
        this.factory = SystemInfoFactory.getInstance();
    }
    
    /**
     * Gets file system information with caching
     */
    public List<FileSystemInfo> getFileSystemInfo() {
        long now = System.currentTimeMillis();
        if (now - lastFileSystemFetch < CACHE_DURATION_MS && !lastFileSystemCache.isEmpty()) {
            return new ArrayList<>(lastFileSystemCache); // Return defensive copy
        }
        
        List<FileSystemInfo> filesystems = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
        
        for (OSFileStore fs : fileSystem.getFileStores()) {
            try {
                FileSystemInfo fsInfo = factory.createFileSystemInfo(fs);
                filesystems.add(fsInfo);
            } catch (Exception e) {
                // Skip problematic file systems
                System.err.println("Error reading file system: " + fs.getMount() + " - " + e.getMessage());
            }
        }
        
        lastFileSystemCache = filesystems;
        lastFileSystemFetch = now;
        return filesystems;
    }
    
    /**
     * Forces cache refresh
     */
    public void refreshCache() {
        lastFileSystemFetch = 0;
        lastFileSystemCache.clear();
    }
    
    /**
     * Gets file systems filtered by type
     */
    public List<FileSystemInfo> getFileSystemsByType(FileSystemInfo.FileSystemType type) {
        return getFileSystemInfo().stream()
                .filter(fs -> fs.getFileSystemType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets critical file systems (>90% full)
     */
    public List<FileSystemInfo> getCriticalFileSystems() {
        return getFileSystemInfo().stream()
                .filter(fs -> fs.isCritical())
                .collect(Collectors.toList());
    }
    
    /**
     * Gets total storage summary
     */
    public StorageSummary getStorageSummary() {
        List<FileSystemInfo> allFS = getFileSystemInfo();
        long totalStorage = 0;
        long usedStorage = 0;
        
        for (FileSystemInfo fs : allFS) {
            totalStorage += fs.getTotalSpaceBytes();
            usedStorage += fs.getUsedSpaceBytes();
        }
        
        return new StorageSummary(totalStorage, usedStorage, allFS.size());
    }
    
    /**
     * Storage summary data class
     */
    public static class StorageSummary {
        private final long totalBytes;
        private final long usedBytes;
        private final int fileSystemCount;
        
        public StorageSummary(long totalBytes, long usedBytes, int fileSystemCount) {
            this.totalBytes = totalBytes;
            this.usedBytes = usedBytes;
            this.fileSystemCount = fileSystemCount;
        }
        
        public long getTotalBytes() { return totalBytes; }
        public long getUsedBytes() { return usedBytes; }
        public long getAvailableBytes() { return totalBytes - usedBytes; }
        public int getFileSystemCount() { return fileSystemCount; }
        
        public double getUsagePercentage() {
            return totalBytes > 0 ? (double) usedBytes / totalBytes * 100.0 : 0.0;
        }
        
        public String getFormattedTotal() {
            return formatBytes(totalBytes);
        }
        
        public String getFormattedUsed() {
            return formatBytes(usedBytes);
        }
        
        public String getFormattedAvailable() {
            return formatBytes(getAvailableBytes());
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp-1) + "";
            return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
        }
    }
}
