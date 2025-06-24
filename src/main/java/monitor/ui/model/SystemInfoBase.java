package monitor.ui.model;

/**
 * Abstract base class for all system information entities
 * Demonstrates Abstraction principle - provides common functionality
 * and defines contract for all system info classes
 */
public abstract class SystemInfoBase {
    protected final String name;
    private final long createdTimestamp;
    
    /**
     * Constructor with common fields
     * @param name The name/identifier of this system info entity
     */
    public SystemInfoBase(String name) {
        this.name = name != null ? name : "Unknown";
        this.createdTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the name/identifier
     * @return The name of this entity
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get creation timestamp
     * @return When this entity was created (in milliseconds)
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    /**
     * Get age in seconds since creation
     * @return Age in seconds
     */
    public long getAgeInSeconds() {
        return (System.currentTimeMillis() - createdTimestamp) / 1000;
    }
    
    /**
     * Abstract method - each subclass must provide its own display name format
     * @return Formatted display name for UI
     */
    public abstract String getDisplayName();
    
    /**
     * Abstract method - each subclass must provide formatted information
     * @return Formatted information string for display
     */
    public abstract String getFormattedInfo();
    
    /**
     * Check if this entity is valid (has valid data)
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }
    
    /**
     * Get a string representation suitable for debugging
     * @return Debug string
     */
    @Override
    public String toString() {
        return String.format("%s{name='%s', created=%d}", 
                           getClass().getSimpleName(), name, createdTimestamp);
    }
    
    /**
     * Default equals implementation based on name
     * Subclasses can override for more specific comparison
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SystemInfoBase that = (SystemInfoBase) obj;
        return name.equals(that.name);
    }
    
    /**
     * Default hashCode implementation based on name
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}