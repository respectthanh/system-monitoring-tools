package monitor.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import monitor.ui.ModernUIManager;
import org.controlsfx.control.ToggleSwitch;

/**
 * Modern top bar component with theme controls and application actions
 */
public class ModernTopBar extends VBox {
    
    private final Runnable onRefresh;
    private final Runnable onToggleAutoRefresh;
    private boolean autoRefreshEnabled = true;
    
    public ModernTopBar(Runnable onRefresh, Runnable onToggleAutoRefresh) {
        this.onRefresh = onRefresh;
        this.onToggleAutoRefresh = onToggleAutoRefresh;
        
        setupTopBar();
        applyModernStyling();
    }
    
    private void setupTopBar() {
        // Main title section
        Label titleLabel = new Label("System Monitor Pro");
        titleLabel.getStyleClass().addAll("h2", "title-label");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 600;");
        
        // Create controls section
        HBox controlsBox = createControlsSection();
        
        // Main layout
        HBox topBarContent = new HBox(20);
        topBarContent.setAlignment(Pos.CENTER_LEFT);
        topBarContent.setPadding(new Insets(16, 20, 16, 20));
        
        // Add spacer to push controls to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        topBarContent.getChildren().addAll(titleLabel, spacer, controlsBox);
        
        // Add separator
        Separator separator = new Separator();
        separator.getStyleClass().add("modern-separator");
        
        this.getChildren().addAll(topBarContent, separator);
    }
    
    private HBox createControlsSection() {
        HBox controlsBox = new HBox(15);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);
        
        // Theme selector
        ComboBox<ModernUIManager.Theme> themeSelector = createThemeSelector();
        
        // Auto-refresh toggle
        Label autoRefreshLabel = new Label("Auto Refresh");
        autoRefreshLabel.setStyle("-fx-font-size: 12px;");
        
        ToggleSwitch autoRefreshToggle = new ToggleSwitch();
        autoRefreshToggle.setSelected(autoRefreshEnabled);
        autoRefreshToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            autoRefreshEnabled = newVal;
            if (onToggleAutoRefresh != null) {
                onToggleAutoRefresh.run();
            }
        });
        
        VBox autoRefreshContainer = new VBox(4);
        autoRefreshContainer.setAlignment(Pos.CENTER);
        autoRefreshContainer.getChildren().addAll(autoRefreshLabel, autoRefreshToggle);
        
        // Manual refresh button
        Button refreshButton = new Button("Refresh");
        refreshButton.setGraphic(ModernUIManager.Icons.refresh());
        refreshButton.getStyleClass().addAll("button", "success");
        refreshButton.setOnAction(e -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
        });
        
        // Theme toggle button
        Button themeToggleButton = new Button();
        themeToggleButton.setGraphic(ModernUIManager.Icons.theme());
        themeToggleButton.getStyleClass().addAll("button", "secondary");
        themeToggleButton.setTooltip(new Tooltip("Toggle Light/Dark Theme"));
        themeToggleButton.setOnAction(e -> ModernUIManager.toggleTheme());
        
        controlsBox.getChildren().addAll(
            new Label("Theme:"), themeSelector,
            new Separator(),
            autoRefreshContainer,
            refreshButton,
            themeToggleButton
        );
        
        return controlsBox;
    }
    
    private ComboBox<ModernUIManager.Theme> createThemeSelector() {
        ComboBox<ModernUIManager.Theme> themeSelector = new ComboBox<>();
        themeSelector.getItems().addAll(ModernUIManager.getAvailableThemes());
        themeSelector.setValue(ModernUIManager.getCurrentTheme());
        
        // Custom cell factory for better display
        themeSelector.setCellFactory(param -> new ListCell<ModernUIManager.Theme>() {
            @Override
            protected void updateItem(ModernUIManager.Theme item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        
        themeSelector.setButtonCell(new ListCell<ModernUIManager.Theme>() {
            @Override
            protected void updateItem(ModernUIManager.Theme item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getDisplayName());
                }
            }
        });
        
        themeSelector.setOnAction(e -> {
            ModernUIManager.Theme selectedTheme = themeSelector.getValue();
            if (selectedTheme != null) {
                ModernUIManager.applyTheme(selectedTheme);
            }
        });
        
        return themeSelector;
    }
    
    private void applyModernStyling() {
        this.getStyleClass().add("top-bar");
        this.setStyle("""
            -fx-background-color: -fx-bg-primary;
            -fx-border-color: transparent transparent -fx-bg-tertiary transparent;
            -fx-border-width: 0 0 1px 0;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);
        """);
    }
    
    public boolean isAutoRefreshEnabled() {
        return autoRefreshEnabled;
    }
    
    public void setAutoRefreshEnabled(boolean enabled) {
        this.autoRefreshEnabled = enabled;
        // Update toggle switch if needed
    }
} 