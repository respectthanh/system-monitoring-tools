package monitor.ui.model;

/**
 * Enhanced ResourceInfo class with OOP principles
 * Demonstrates Inheritance and Interface Implementation
 */
public class ResourceInfo extends SystemInfoBase implements Monitorable {
    private final String status;
    private final String used;
    private final String total;
    private final double usedPercent;

    public ResourceInfo(String name, String status, String used, String total, double usedPercent) {
        super(name);
        this.status = status;
        this.used = used;
        this.total = total;
        this.usedPercent = usedPercent;
    }

    // Getters with proper encapsulation
    public String getUsed() { return used; }
    public String getTotal() { return total; }
    public double getUsedPercent() { return usedPercent; }

    // Implementation of abstract methods from SystemInfoBase
    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getFormattedInfo() {
        return String.format("%s: %s / %s (%.2f%%)", name, used, total, usedPercent);
    }

    // Implementation of Monitorable interface
    @Override
    public double getUsagePercentage() {
        return usedPercent;
    }

    @Override
    public String getStatus() {
        return status; // Use the provided status
    }
    
    // Additional method for resource-specific functionality
    public boolean isMemoryResource() {
        return name.toLowerCase().contains("memory") || name.toLowerCase().contains("ram");
    }
    
    public boolean isCpuResource() {
        return name.toLowerCase().contains("cpu") || name.toLowerCase().contains("core");
    }
    
    public boolean isSwapResource() {
        return name.toLowerCase().contains("swap");
    }
    
    /**
     * Gets resource type for categorization
     */
    public ResourceType getResourceType() {
        if (isCpuResource()) return ResourceType.CPU;
        if (isMemoryResource()) return ResourceType.MEMORY;
        if (isSwapResource()) return ResourceType.SWAP;
        return ResourceType.OTHER;
    }
    
    /**
     * Enum for resource types - demonstrates encapsulation
     */
    public enum ResourceType {
        CPU("Central Processing Unit"),
        MEMORY("Random Access Memory"),
        SWAP("Virtual Memory"),
        OTHER("Other Resource");
        
        private final String description;
        
        ResourceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
