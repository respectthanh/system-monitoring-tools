package monitor.ui.model;

/**
 * Enhanced ProcessInfo class with OOP principles
 * Demonstrates Inheritance, Encapsulation, and Interface Implementation
 */
public class ProcessInfo extends SystemInfoBase implements Monitorable, Terminatable {
    private final String user;
    private final String pid;
    private final double cpuValue;
    private final double rssValue; 
    private final double virtualMemValue;
    private final double diskReadValue;
    
    // Formatted strings for display (lazy loading for better performance)
    private String formattedCpu;
    private String formattedRss;
    private String formattedVirtualMem;
    private String formattedDiskRead;

    public ProcessInfo(String name, String user, String pid, 
                      double cpuValue, double rssValue, 
                      double virtualMemValue, double diskReadValue) {
        super(name);
        this.user = user;
        this.pid = pid;
        this.cpuValue = cpuValue;
        this.rssValue = rssValue;
        this.virtualMemValue = virtualMemValue;
        this.diskReadValue = diskReadValue;
    }

    // Getters with encapsulation
    public String getUser() { return user; }
    public String getPid() { return pid; }
    public Double getCpuValue() { return cpuValue; }
    public Double getRssValue() { return rssValue; }
    public Double getVirtualMemValue() { return virtualMemValue; }
    public Double getDiskReadValue() { return diskReadValue; }
    
    // Lazy loading for formatted strings (optimization)
    public String getCpu() {
        if (formattedCpu == null) {
            formattedCpu = String.format("%.2f", cpuValue);
        }
        return formattedCpu;
    }
    
    public String getRss() {
        if (formattedRss == null) {
            formattedRss = String.format("%.2f", rssValue);
        }
        return formattedRss;
    }
    
    public String getVirtualMem() {
        if (formattedVirtualMem == null) {
            formattedVirtualMem = String.format("%.2f", virtualMemValue);
        }
        return formattedVirtualMem;
    }
    
    public String getDiskRead() {
        if (formattedDiskRead == null) {
            formattedDiskRead = String.format("%.2f", diskReadValue);
        }
        return formattedDiskRead;
    }

    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        return String.format("%s (PID: %s)", name, pid);
    }

    @Override
    public String getFormattedInfo() {
        return String.format("CPU: %s%%, Memory: %s MB, User: %s", 
                           getCpu(), getRss(), user);
    }

    // Implementation of Monitorable interface
    @Override
    public double getUsagePercentage() {
        return cpuValue; // Use CPU usage as primary metric
    }

    @Override
    public String getStatus() {
        double cpu = getUsagePercentage();
        if (cpu > 80) return "High";
        if (cpu > 40) return "Medium";
        return "Low";
    }

    // Implementation of Terminatable interface
    @Override
    public String getTerminationId() {
        return pid;
    }

    @Override
    public String getTerminationCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "taskkill /F /PID " + pid;
        } else {
            return "kill -9 " + pid;
        }
    }

    @Override
    public boolean canTerminate() {
        // Don't allow termination of system critical processes
        return !isSystemCritical();
    }
    
    @Override
    public String getTerminationConfirmMessage() {
        return String.format("Are you sure you want to terminate process '%s' (PID: %s)?", 
                           name, pid);
    }
    
    // Helper method to determine if process is system critical
    private boolean isSystemCritical() {
        String[] criticalProcesses = {"init", "kernel", "systemd", "kthreadd"};
        String processName = name.toLowerCase();
        for (String critical : criticalProcesses) {
            if (processName.contains(critical)) {
                return true;
            }
        }
        return false;
    }
    
    // Override equals to use PID for comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProcessInfo that = (ProcessInfo) obj;
        return pid.equals(that.pid);
    }
    
    @Override
    public int hashCode() {
        return pid.hashCode();
    }
}
