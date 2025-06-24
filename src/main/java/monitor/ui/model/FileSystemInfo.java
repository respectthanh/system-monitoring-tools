package monitor.ui.model;

/**
 * Enhanced FileSystemInfo class with OOP principles
 * Demonstrates Inheritance and Interface Implementation
 */
public class FileSystemInfo extends SystemInfoBase implements Monitorable {
    private final String mountPoint;
    private final String type;
    private final long totalSpaceBytes;
    private final long usedSpaceBytes;
    private final long usableSpaceBytes;
    
    // Cached formatted strings for performance
    private String formattedTotalSpace;
    private String formattedUsedSpace;
    private String formattedUsableSpace;

    public FileSystemInfo(String mountPoint, String name, String type, 
                         long totalSpace, long usedSpace, long usableSpace) {
        super(name);
        this.mountPoint = mountPoint;
        this.type = type;
        this.totalSpaceBytes = totalSpace;
        this.usedSpaceBytes = usedSpace;
        this.usableSpaceBytes = usableSpace;
    }

    // Getters with proper encapsulation
    public String getMountPoint() { return mountPoint; }
    public String getType() { return type; }
    public long getTotalSpaceBytes() { return totalSpaceBytes; }
    public long getUsedSpaceBytes() { return usedSpaceBytes; }
    public long getUsableSpaceBytes() { return usableSpaceBytes; }
    
    // Lazy loading for formatted strings
    public String getTotalSpace() {
        if (formattedTotalSpace == null) {
            formattedTotalSpace = formatBytes(totalSpaceBytes);
        }
        return formattedTotalSpace;
    }
    
    public String getUsedSpace() {
        if (formattedUsedSpace == null) {
            formattedUsedSpace = formatBytes(usedSpaceBytes);
        }
        return formattedUsedSpace;
    }
    
    public String getUsableSpace() {
        if (formattedUsableSpace == null) {
            formattedUsableSpace = formatBytes(usableSpaceBytes);
        }
        return formattedUsableSpace;
    }

    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        return String.format("%s (%s)", mountPoint, name);
    }

    @Override
    public String getFormattedInfo() {
        return String.format("%s - %s: %s / %s (%.1f%% used)", 
                           mountPoint, type, getUsedSpace(), getTotalSpace(), getUsagePercentage());
    }

    // Implementation of Monitorable interface
    @Override
    public double getUsagePercentage() {
        if (totalSpaceBytes == 0) return 0.0;
        return (double) usedSpaceBytes / totalSpaceBytes * 100.0;
    }

    @Override
    public String getStatus() {
        double usage = getUsagePercentage();
        if (usage > 95) return "Critical";
        if (usage > 80) return "High";
        if (usage > 60) return "Medium";
        return "Low";
    }
    
    // Helper method for formatting bytes - demonstrates encapsulation
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // Additional utility methods
    public boolean isSystemDrive() {
        return "/".equals(mountPoint) || "C:".equals(mountPoint);
    }
    
    public boolean isRemovable() {
        return type.toLowerCase().contains("usb") || 
               type.toLowerCase().contains("removable") ||
               mountPoint.startsWith("/media/") ||
               mountPoint.startsWith("/mnt/");
    }
    
    public boolean isNetworkDrive() {
        return type.toLowerCase().contains("nfs") || 
               type.toLowerCase().contains("cifs") ||
               type.toLowerCase().contains("smb");
    }
    
    /**
     * Gets the file system category
     */
    public FileSystemType getFileSystemType() {
        if (isSystemDrive()) return FileSystemType.SYSTEM;
        if (isRemovable()) return FileSystemType.REMOVABLE;
        if (isNetworkDrive()) return FileSystemType.NETWORK;
        return FileSystemType.LOCAL;
    }
    
    /**
     * Enum for file system types
     */
    public enum FileSystemType {
        SYSTEM("System Drive"),
        LOCAL("Local Drive"),
        REMOVABLE("Removable Drive"),
        NETWORK("Network Drive");
        
        private final String description;
        
        FileSystemType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
