package monitor.ui.model;

/**
 * Interface for entities that can be terminated/killed
 */
public interface Terminatable {
    /**
     * Gets the unique identifier for termination
     */
    String getTerminationId();
    
    /**
     * Gets the command used for termination
     */
    String getTerminationCommand();
    
    /**
     * Checks if termination is allowed
     */
    boolean canTerminate();
    
    /**
     * Gets confirmation message for termination
     */
    default String getTerminationConfirmMessage() {
        return String.format("Are you sure you want to terminate '%s'?", getTerminationId());
    }
}
