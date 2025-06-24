package monitor.ui.service;

import monitor.ui.SystemdStartupDetector;
import monitor.ui.model.StartupInfo;
import monitor.ui.model.StartupGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for startup applications management
 * Demonstrates Service Layer pattern, Grouping, and Caching
 */
public class StartupService {
    
    private final SystemInfoFactory factory;
    private long lastStartupFetch = 0;
    private List<StartupInfo> lastStartupCache = new ArrayList<>();
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache
    
    public StartupService() {
        this.factory = SystemInfoFactory.getInstance();
    }
    
    /**
     * Gets all startup applications with caching
     */
    public List<StartupInfo> getStartupApplications() {
        long now = System.currentTimeMillis();
        if (now - lastStartupFetch < CACHE_DURATION_MS && !lastStartupCache.isEmpty()) {
            return new ArrayList<>(lastStartupCache); // Return defensive copy
        }
        
        List<StartupInfo> startupApps = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            startupApps.addAll(getWindowsStartupApps());
        } else if (osName.contains("linux")) {
            startupApps.addAll(getLinuxStartupApps());
        }

        lastStartupCache = startupApps;
        lastStartupFetch = now;
        return startupApps;
    }
    
    /**
     * Gets startup applications grouped by directory
     */
    public List<StartupGroup> getGroupedStartupApplications() {
        List<StartupInfo> allApps = getStartupApplications();
        
        // Group by directory
        Map<String, List<StartupInfo>> directoryGroups = allApps.stream()
                .collect(Collectors.groupingBy(StartupInfo::getGroupKey));
        
        List<StartupGroup> groups = new ArrayList<>();
        
        for (Map.Entry<String, List<StartupInfo>> entry : directoryGroups.entrySet()) {
            String directory = entry.getKey();
            List<StartupInfo> items = entry.getValue();
            
            if (items.size() == 1) {
                // Single item - add directly
                StartupGroup singleItemGroup = factory.createStartupGroup(items.get(0));
                groups.add(singleItemGroup);
            } else {
                // Multiple items - create directory group
                StartupGroup directoryGroup = factory.createStartupGroup(directory);
                for (StartupInfo item : items) {
                    directoryGroup.addItem(item);
                }
                groups.add(directoryGroup);
            }
        }
        
        return groups;
    }
    
    /**
     * Gets startup applications by type
     */
    public List<StartupInfo> getStartupApplicationsByType(StartupInfo.StartupType type) {
        return getStartupApplications().stream()
                .filter(app -> app.getStartupType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Forces cache refresh
     */
    public void refreshCache() {
        lastStartupFetch = 0;
        lastStartupCache.clear();
    }
    
    /**
     * Gets startup statistics
     */
    public StartupStatistics getStartupStatistics() {
        List<StartupInfo> allApps = getStartupApplications();
        
        long systemServices = allApps.stream().mapToLong(app -> app.isSystemService() ? 1 : 0).sum();
        long userApps = allApps.stream().mapToLong(app -> app.isUserApplication() ? 1 : 0).sum();
        long cronJobs = allApps.stream().mapToLong(app -> app.isCronJob() ? 1 : 0).sum();
        long other = allApps.size() - systemServices - userApps - cronJobs;
        
        return new StartupStatistics(allApps.size(), systemServices, userApps, cronJobs, other);
    }
    
    // Private helper methods
    private List<StartupInfo> getWindowsStartupApps() {
        List<StartupInfo> apps = new ArrayList<>();
        String userStartupFolder = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
        String allUsersStartupFolder = System.getenv("PROGRAMDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
        
        apps.addAll(getStartupAppsFromFolder(userStartupFolder));
        apps.addAll(getStartupAppsFromFolder(allUsersStartupFolder));
        
        return apps;
    }
    
    private List<StartupInfo> getLinuxStartupApps() {
        List<StartupInfo> apps = new ArrayList<>();
        
        // Desktop autostart files
        String userAutostartFolder = System.getProperty("user.home") + "/.config/autostart";
        String systemAutostartFolder = "/etc/xdg/autostart";
        apps.addAll(getStartupAppsFromFolder(userAutostartFolder));
        apps.addAll(getStartupAppsFromFolder(systemAutostartFolder));
        
        // Systemd services
        SystemdStartupDetector systemdDetector = new SystemdStartupDetector();
        apps.addAll(systemdDetector.getSystemdStartupServices());
        apps.addAll(systemdDetector.getCronJobsAtReboot());
        apps.addAll(systemdDetector.getRcLocalEntries());
        
        return apps;
    }
    
    private List<StartupInfo> getStartupAppsFromFolder(String folderPath) {
        List<StartupInfo> apps = new ArrayList<>();
        File folder = new File(folderPath);
        
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> 
                name.endsWith(".desktop") || name.endsWith(".lnk"));
            
            if (files != null) {
                for (File file : files) {
                    try {
                        StartupInfo startupInfo = factory.createStartupInfo(file.getName(), file.getAbsolutePath());
                        apps.add(startupInfo);
                    } catch (Exception e) {
                        System.err.println("Error reading startup file: " + file.getAbsolutePath() + " - " + e.getMessage());
                    }
                }
            }
        }
        return apps;
    }
    
    /**
     * Startup statistics data class
     */
    public static class StartupStatistics {
        private final int totalCount;
        private final long systemServices;
        private final long userApplications;
        private final long cronJobs;
        private final long other;
        
        public StartupStatistics(int totalCount, long systemServices, long userApplications, long cronJobs, long other) {
            this.totalCount = totalCount;
            this.systemServices = systemServices;
            this.userApplications = userApplications;
            this.cronJobs = cronJobs;
            this.other = other;
        }
        
        public int getTotalCount() { return totalCount; }
        public long getSystemServices() { return systemServices; }
        public long getUserApplications() { return userApplications; }
        public long getCronJobs() { return cronJobs; }
        public long getOther() { return other; }
        
        public double getSystemServicesPercentage() {
            return totalCount > 0 ? (double) systemServices / totalCount * 100.0 : 0.0;
        }
        
        public double getUserApplicationsPercentage() {
            return totalCount > 0 ? (double) userApplications / totalCount * 100.0 : 0.0;
        }
    }
}
