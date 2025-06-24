package monitor.ui.model;

/**
 * Interface for objects that can be monitored
 * Demonstrates Interface Segregation Principle
 */
public interface Monitorable {
    /**
     * Gets the current usage percentage (0-100)
     */
    double getUsagePercentage();
    
    /**
     * Gets the status based on usage level
     */
    String getStatus();
    
    /**
     * Checks if the usage is in critical state
     */
    default boolean isCritical() {
        return getUsagePercentage() > 90.0;
    }
    
    /**
     * Gets color code based on usage level
     */
    default String getStatusColor() {
        double usage = getUsagePercentage();
        if (usage < 40) return "#4caf50"; // green
        if (usage < 70) return "#ffeb3b"; // yellow
        if (usage < 90) return "#ff9800"; // orange
        return "#f44336"; // red
    }
}
