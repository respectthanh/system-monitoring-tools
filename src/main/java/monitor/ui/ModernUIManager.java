package monitor.ui;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.image.Image;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.feather.Feather;

/**
 * Modern UI Manager for System Monitoring Tool
 * Handles theme management, styling, and modern UI enhancements
 */
public class ModernUIManager {
    
    public enum Theme {
        PRIMER_LIGHT("Primer Light", new PrimerLight()),
        PRIMER_DARK("Primer Dark", new PrimerDark()),
        NORD_LIGHT("Nord Light", new NordLight()),
        NORD_DARK("Nord Dark", new NordDark());
        
        private final String displayName;
        private final atlantafx.base.theme.Theme theme;
        
        Theme(String displayName, atlantafx.base.theme.Theme theme) {
            this.displayName = displayName;
            this.theme = theme;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public atlantafx.base.theme.Theme getTheme() {
            return theme;
        }
    }
    
    private static Theme currentTheme = Theme.PRIMER_LIGHT;
    private static Scene currentScene;
    
    /**
     * Initialize the modern UI with default theme
     */
    public static void initialize() {
        // Set default theme
        applyTheme(currentTheme);
    }
    
    /**
     * Apply a specific theme to the application
     */
    public static void applyTheme(Theme theme) {
        currentTheme = theme;
        Application.setUserAgentStylesheet(theme.getTheme().getUserAgentStylesheet());
        
        // Apply custom CSS enhancements
        if (currentScene != null) {
            currentScene.getStylesheets().clear();
            currentScene.getStylesheets().add(
                ModernUIManager.class.getResource("/styles/modern-theme.css").toExternalForm()
            );
            
            // Apply theme-specific root style class
            if (theme.name().contains("DARK")) {
                currentScene.getRoot().getStyleClass().add("dark");
            } else {
                currentScene.getRoot().getStyleClass().remove("dark");
            }
        }
    }
    
    /**
     * Setup the main stage with modern styling
     */
    public static void setupStage(Stage stage) {
        // Set application icon
        try {
            stage.getIcons().add(new Image(
                ModernUIManager.class.getResourceAsStream("/icons/app-icon.png")
            ));
        } catch (Exception e) {
            // Icon not found, continue without it
        }
        
        // Set minimum size
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        
        // Center on screen
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - 1200) / 2);
        stage.setY((screenBounds.getHeight() - 800) / 2);
        
        // Set default size
        stage.setWidth(1200);
        stage.setHeight(800);
        
        // Modern title
        stage.setTitle("System Monitor Pro");
    }
    
    /**
     * Setup scene with modern styling
     */
    public static void setupScene(Scene scene) {
        currentScene = scene;
        
        // Apply custom CSS
        scene.getStylesheets().add(
            ModernUIManager.class.getResource("/styles/modern-theme.css").toExternalForm()
        );
        
        // Apply theme-specific styling
        applyTheme(currentTheme);
    }
    
    /**
     * Get the current theme
     */
    public static Theme getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Toggle between light and dark themes
     */
    public static void toggleTheme() {
        switch (currentTheme) {
            case PRIMER_LIGHT:
                applyTheme(Theme.PRIMER_DARK);
                break;
            case PRIMER_DARK:
                applyTheme(Theme.PRIMER_LIGHT);
                break;
            case NORD_LIGHT:
                applyTheme(Theme.NORD_DARK);
                break;
            case NORD_DARK:
                applyTheme(Theme.NORD_LIGHT);
                break;
        }
    }
    
    /**
     * Create modern icons for UI elements
     */
    public static class Icons {
        public static FontIcon monitor() {
            FontIcon icon = new FontIcon(MaterialDesignM.MONITOR);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon cpu() {
            FontIcon icon = new FontIcon(MaterialDesignC.CPU_64_BIT);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon memory() {
            FontIcon icon = new FontIcon(MaterialDesignM.MEMORY);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon storage() {
            FontIcon icon = new FontIcon(Feather.HARD_DRIVE);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon process() {
            FontIcon icon = new FontIcon(Feather.ACTIVITY);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon settings() {
            FontIcon icon = new FontIcon(Feather.SETTINGS);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon refresh() {
            FontIcon icon = new FontIcon(Feather.REFRESH_CW);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon stop() {
            FontIcon icon = new FontIcon(Feather.SQUARE);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon play() {
            FontIcon icon = new FontIcon(Feather.PLAY);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon theme() {
            FontIcon icon = new FontIcon(Feather.MOON);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon chart() {
            FontIcon icon = new FontIcon(Feather.BAR_CHART_2);
            icon.setIconSize(16);
            return icon;
        }
        
        public static FontIcon startup() {
            FontIcon icon = new FontIcon(Feather.POWER);
            icon.setIconSize(16);
            return icon;
        }
    }
    
    /**
     * Get all available themes
     */
    public static Theme[] getAvailableThemes() {
        return Theme.values();
    }
    
    /**
     * Apply modern styling to a specific node
     */
    public static void applyCardStyle(javafx.scene.Node node) {
        node.getStyleClass().add("card");
    }
    
    /**
     * Apply fade-in animation to a node
     */
    public static void applyFadeIn(javafx.scene.Node node) {
        node.getStyleClass().add("fade-in");
    }
} 