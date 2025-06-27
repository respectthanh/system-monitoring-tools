package monitor.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import monitor.ui.ModernUIManager;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Modern dashboard card component for displaying system metrics
 */
public class DashboardCard extends VBox {
    
    private final Label titleLabel;
    private final Label valueLabel;
    private final Label unitLabel;
    private final ProgressBar progressBar;
    private final FontIcon icon;
    
    public DashboardCard(String title, FontIcon icon) {
        this.icon = icon;
        this.titleLabel = new Label(title);
        this.valueLabel = new Label("0");
        this.unitLabel = new Label("");
        this.progressBar = new ProgressBar(0);
        
        setupCard();
        applyModernStyling();
    }
    
    private void setupCard() {
        // Header with icon and title
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        icon.setIconSize(20);
        icon.getStyleClass().add("card-icon");
        
        titleLabel.getStyleClass().addAll("card-title");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -fx-text-secondary;");
        
        header.getChildren().addAll(icon, titleLabel);
        
        // Value section
        HBox valueSection = new HBox(4);
        valueSection.setAlignment(Pos.BASELINE_LEFT);
        
        valueLabel.getStyleClass().add("card-value");
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 600; -fx-text-fill: -fx-text-primary;");
        
        unitLabel.getStyleClass().add("card-unit");
        unitLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 400; -fx-text-fill: -fx-text-secondary;");
        
        valueSection.getChildren().addAll(valueLabel, unitLabel);
        
        // Progress bar
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("modern-progress");
        
        // Layout
        setSpacing(12);
        setPadding(new Insets(16));
        setAlignment(Pos.TOP_LEFT);
        setPrefWidth(200);
        setMinHeight(120);
        
        getChildren().addAll(header, valueSection, progressBar);
    }
    
    private void applyModernStyling() {
        getStyleClass().add("dashboard-card");
        setStyle("""
            -fx-background-color: -fx-bg-primary;
            -fx-background-radius: 12px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);
            -fx-border-color: -fx-bg-tertiary;
            -fx-border-width: 0.5px;
            -fx-border-radius: 12px;
        """);
        
        // Hover effect
        setOnMouseEntered(e -> {
            setStyle(getStyle() + """
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4);
                -fx-scale-x: 1.02;
                -fx-scale-y: 1.02;
            """);
        });
        
        setOnMouseExited(e -> {
            setStyle("""
                -fx-background-color: -fx-bg-primary;
                -fx-background-radius: 12px;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);
                -fx-border-color: -fx-bg-tertiary;
                -fx-border-width: 0.5px;
                -fx-border-radius: 12px;
                -fx-scale-x: 1.0;
                -fx-scale-y: 1.0;
            """);
        });
    }
    
    public void updateValue(String value, String unit) {
        valueLabel.setText(value);
        unitLabel.setText(unit);
    }
    
    public void updateProgress(double progress) {
        progressBar.setProgress(progress);
        
        // Color coding based on usage
        if (progress > 0.9) {
            progressBar.getStyleClass().removeAll("warning", "success");
            progressBar.getStyleClass().add("danger");
        } else if (progress > 0.7) {
            progressBar.getStyleClass().removeAll("danger", "success");
            progressBar.getStyleClass().add("warning");
        } else {
            progressBar.getStyleClass().removeAll("danger", "warning");
            progressBar.getStyleClass().add("success");
        }
    }
    
    public void updateTitle(String title) {
        titleLabel.setText(title);
    }
    
    public void setIcon(FontIcon newIcon) {
        if (getChildren().size() > 0 && getChildren().get(0) instanceof HBox header) {
            if (header.getChildren().size() > 0) {
                header.getChildren().set(0, newIcon);
            }
        }
    }
} 