package monitor.ui.model;

/**
 * Model class representing GPU information
 * Implements Monitorable interface for consistent monitoring pattern
 */
public class GPUInfo extends SystemInfoBase implements Monitorable {
    
    private final String vendor;
    private final long totalMemory;
    private final long usedMemory;
    private final double memoryUsagePercent;
    private final double gpuUtilization;
    private final double temperature;
    private final int fanSpeed;
    private final long clockSpeed;
    private final long memoryClockSpeed;
    private final String driverVersion;
    private final boolean isDiscrete;
    
    // Constructor for comprehensive GPU information
    public GPUInfo(String name, String vendor, long totalMemory, long usedMemory, 
                   double gpuUtilization, double temperature, int fanSpeed,
                   long clockSpeed, long memoryClockSpeed, String driverVersion, boolean isDiscrete) {
        super(name != null ? name : "Unknown GPU");
        this.vendor = vendor != null ? vendor : "Unknown";
        this.totalMemory = Math.max(0, totalMemory);
        this.usedMemory = Math.max(0, Math.min(usedMemory, totalMemory));
        this.memoryUsagePercent = totalMemory > 0 ? (double) this.usedMemory / totalMemory * 100.0 : 0.0;
        this.gpuUtilization = Math.max(0.0, Math.min(100.0, gpuUtilization));
        this.temperature = Math.max(0.0, temperature);
        this.fanSpeed = Math.max(0, fanSpeed);
        this.clockSpeed = Math.max(0, clockSpeed);
        this.memoryClockSpeed = Math.max(0, memoryClockSpeed);
        this.driverVersion = driverVersion != null ? driverVersion : "Unknown";
        this.isDiscrete = isDiscrete;
    }
    
    // Simplified constructor for basic GPU info
    public GPUInfo(String name, String vendor) {
        this(name, vendor, 0, 0, 0.0, 0.0, 0, 0, 0, "Unknown", false);
    }
    
    // Getters
    public String getVendor() { return vendor; }
    public long getTotalMemory() { return totalMemory; }
    public long getUsedMemory() { return usedMemory; }
    public long getAvailableMemory() { return totalMemory - usedMemory; }
    public double getMemoryUsagePercent() { return memoryUsagePercent; }
    public double getGpuUtilization() { return gpuUtilization; }
    public double getTemperature() { return temperature; }
    public int getFanSpeed() { return fanSpeed; }
    public long getClockSpeed() { return clockSpeed; }
    public long getMemoryClockSpeed() { return memoryClockSpeed; }
    public String getDriverVersion() { return driverVersion; }
    public boolean isDiscrete() { return isDiscrete; }
    
    // Formatted getters for UI display
    public String getFormattedTotalMemory() {
        return formatBytes(totalMemory);
    }
    
    public String getFormattedUsedMemory() {
        return formatBytes(usedMemory);
    }
    
    public String getFormattedAvailableMemory() {
        return formatBytes(getAvailableMemory());
    }
    
    public String getFormattedClockSpeed() {
        return clockSpeed > 0 ? String.format("%d MHz", clockSpeed) : "N/A";
    }
    
    public String getFormattedMemoryClockSpeed() {
        return memoryClockSpeed > 0 ? String.format("%d MHz", memoryClockSpeed) : "N/A";
    }
    
    public String getFormattedTemperature() {
        return temperature > 0 ? String.format("%.1f°C", temperature) : "N/A";
    }
    
    public String getFormattedFanSpeed() {
        return fanSpeed > 0 ? String.format("%d RPM", fanSpeed) : "N/A";
    }
    
    public String getGpuType() {
        return isDiscrete ? "Discrete" : "Integrated";
    }
    
    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        return String.format("%s (%s)", name, vendor);
    }
    
    @Override
    public String getFormattedInfo() {
        return String.format("GPU: %.1f%%, Memory: %.1f%%, Temp: %s", 
                           gpuUtilization, memoryUsagePercent, getFormattedTemperature());
    }
    
    // Implementation of Monitorable interface
    @Override
    public double getUsagePercentage() {
        return gpuUtilization;
    }
    
    @Override
    public String getStatus() {
        if (!isHealthy()) {
            return "Critical";
        } else if (gpuUtilization > 80.0 || temperature > 75.0) {
            return "High";
        } else if (gpuUtilization > 40.0 || temperature > 50.0) {
            return "Medium";
        } else {
            return "Low";
        }
    }
    
    // Additional monitoring methods
    public boolean isHealthy() {
        // Consider GPU healthy if temperature is below 85°C and utilization is reasonable
        return temperature == 0.0 || (temperature < 85.0 && gpuUtilization < 100.0);
    }
    
    // Utility method for formatting bytes
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // toString for debugging
    @Override
    public String toString() {
        return String.format("GPUInfo{name='%s', vendor='%s', utilization=%.1f%%, memory=%s/%s, temp=%s}", 
                           name, vendor, gpuUtilization, getFormattedUsedMemory(), 
                           getFormattedTotalMemory(), getFormattedTemperature());
    }
    
    // equals and hashCode for proper collection handling
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        GPUInfo gpuInfo = (GPUInfo) obj;
        return name.equals(gpuInfo.name) && vendor.equals(gpuInfo.vendor);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, vendor);
    }
} 