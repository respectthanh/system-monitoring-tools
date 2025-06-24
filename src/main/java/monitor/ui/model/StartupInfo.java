package monitor.ui.model;

/**
 * Enhanced StartupInfo class with OOP principles
 * Demonstrates Inheritance and Interface Implementation
 */
public class StartupInfo extends SystemInfoBase implements Groupable<StartupInfo> {
    private final String path;
    private String cachedDirectory; // Cache for performance

    public StartupInfo(String name, String path) {
        super(name);
        this.path = path;
    }

    // Getters with proper encapsulation
    public String getPath() { return path; }
    
    public String getDirectory() {
        if (cachedDirectory == null) {
            if (path.contains("/")) {
                cachedDirectory = path.substring(0, path.lastIndexOf('/'));
            } else {
                cachedDirectory = "Unknown";
            }
        }
        return cachedDirectory;
    }

    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getFormattedInfo() {
        return String.format("%s - %s", name, path);
    }

    // Implementation of Groupable interface
    @Override
    public String getGroupKey() {
        return getDirectory();
    }

    @Override
    public String getGroupDisplayName() {
        return getDirectory();
    }

    @Override
    public boolean shouldGroup() {
        // Group if there are other items in the same directory
        return !getDirectory().equals("Unknown");
    }
    
    // Additional utility methods
    public boolean isSystemService() {
        return path.contains("/lib/systemd/") || 
               path.contains("/usr/lib/systemd/") ||
               name.endsWith(".service");
    }
    
    public boolean isUserApplication() {
        return path.contains("/.config/autostart/") ||
               path.contains("/home/");
    }
    
    public boolean isCronJob() {
        return name.toLowerCase().contains("cron") ||
               path.toLowerCase().contains("crontab");
    }
    
    /**
     * Gets the startup type for categorization
     */
    public StartupType getStartupType() {
        if (isSystemService()) return StartupType.SYSTEM_SERVICE;
        if (isUserApplication()) return StartupType.USER_APPLICATION;
        if (isCronJob()) return StartupType.CRON_JOB;
        return StartupType.OTHER;
    }
    
    /**
     * Gets the file extension if available
     */
    public String getFileExtension() {
        if (path.contains(".")) {
            return path.substring(path.lastIndexOf('.') + 1);
        }
        return "";
    }
    
    /**
     * Checks if this is an executable file
     */
    public boolean isExecutable() {
        String ext = getFileExtension().toLowerCase();
        return ext.equals("exe") || ext.equals("sh") || ext.equals("bat") || 
               ext.equals("desktop") || path.contains("/bin/");
    }
    
    /**
     * Enum for startup types
     */
    public enum StartupType {
        SYSTEM_SERVICE("System Service"),
        USER_APPLICATION("User Application"),
        CRON_JOB("Scheduled Task"),
        OTHER("Other");
        
        private final String description;
        
        StartupType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
