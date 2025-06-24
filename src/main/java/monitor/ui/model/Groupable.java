package monitor.ui.model;

import java.util.List;

/**
 * Interface for entities that can be grouped
 */
public interface Groupable<T> {
    /**
     * Gets the grouping key
     */
    String getGroupKey();
    
    /**
     * Gets the display name for the group
     */
    String getGroupDisplayName();
    
    /**
     * Checks if this entity should be grouped with others
     */
    boolean shouldGroup();
    
    /**
     * Gets the list of items in the group (for group entities)
     */
    default List<T> getGroupItems() {
        return List.of();
    }
    
    /**
     * Checks if this is a group entity
     */
    default boolean isGroup() {
        return !getGroupItems().isEmpty();
    }
}
