package monitor.ui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced StartupGroup class with OOP principles
 * Demonstrates Composition and Interface Implementation
 */
public class StartupGroup extends SystemInfoBase implements Groupable<StartupInfo> {
    private final String directoryPath;
    private final List<StartupInfo> items;
    private final boolean isGroup;
    
    // Constructor for group (directory)
    public StartupGroup(String directoryPath) {
        super(getDirectoryName(directoryPath));
        this.directoryPath = directoryPath;
        this.items = new ArrayList<>();
        this.isGroup = true;
    }
    
    // Constructor for individual item
    public StartupGroup(StartupInfo item) {
        super(item.getName());
        this.directoryPath = "";
        this.items = new ArrayList<>();
        this.items.add(item);
        this.isGroup = false;
    }
    
    // Helper method to extract directory name
    private static String getDirectoryName(String directoryPath) {
        if (directoryPath.contains("/")) {
            return directoryPath.substring(directoryPath.lastIndexOf('/') + 1);
        }
        return directoryPath;
    }
    
    public void addItem(StartupInfo item) {
        if (isGroup && item != null) {
            items.add(item);
        }
    }
    
    public String getDisplayPath() {
        if (isGroup) {
            return directoryPath;
        } else {
            return items.isEmpty() ? "" : items.get(0).getPath();
        }
    }
    
    // Getters with proper encapsulation    
    @Override
    public boolean isGroup() { return isGroup; }
    public String getDirectoryPath() { return directoryPath; }
    
    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        if (isGroup) {
            return name + " (" + items.size() + " items)";
        } else {
            return name;
        }
    }

    @Override
    public String getFormattedInfo() {
        if (isGroup) {
            return String.format("Directory: %s (%d items)", directoryPath, items.size());
        } else {
            return items.isEmpty() ? "Empty group" : items.get(0).getFormattedInfo();
        }
    }

    // Implementation of Groupable interface
    @Override
    public String getGroupKey() {
        return directoryPath;
    }

    @Override
    public String getGroupDisplayName() {
        return getDisplayName();
    }

    @Override
    public boolean shouldGroup() {
        return isGroup;
    }
    
    @Override
    public List<StartupInfo> getGroupItems() {
        return new ArrayList<>(items); // Return defensive copy
    }
    
    // Additional utility methods
    public int getItemCount() {
        return items.size();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public boolean hasSystemServices() {
        return items.stream().anyMatch(StartupInfo::isSystemService);
    }
    
    public boolean hasUserApplications() {
        return items.stream().anyMatch(StartupInfo::isUserApplication);
    }
    
    public long getSystemServiceCount() {
        return items.stream().mapToLong(item -> item.isSystemService() ? 1 : 0).sum();
    }
    
    public long getUserApplicationCount() {
        return items.stream().mapToLong(item -> item.isUserApplication() ? 1 : 0).sum();
    }
    
    /**
     * Gets the predominant startup type in this group
     */
    public StartupInfo.StartupType getPredominantType() {
        if (isEmpty()) return StartupInfo.StartupType.OTHER;
        
        // Count occurrences of each type
        long systemCount = getSystemServiceCount();
        long userCount = getUserApplicationCount();
        long cronCount = items.stream().mapToLong(item -> item.isCronJob() ? 1 : 0).sum();
        
        if (systemCount >= userCount && systemCount >= cronCount) {
            return StartupInfo.StartupType.SYSTEM_SERVICE;
        } else if (userCount >= cronCount) {
            return StartupInfo.StartupType.USER_APPLICATION;
        } else if (cronCount > 0) {
            return StartupInfo.StartupType.CRON_JOB;
        }
        
        return StartupInfo.StartupType.OTHER;
    }
}
