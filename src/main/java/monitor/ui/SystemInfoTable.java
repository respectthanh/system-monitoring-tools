package monitor.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Import new OOP model classes
import monitor.ui.model.*;
import monitor.ui.service.*;
import monitor.ui.components.ModernTopBar;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import monitor.ui.components.DashboardCard;
import javafx.util.Duration;

public class SystemInfoTable extends Application {
    // OOP Services - demonstrates Dependency Injection and Service Layer pattern
    private final ProcessService processService;
    private final ResourceMonitoringService resourceService;
    private final FileSystemService fileSystemService;
    private final StartupService startupService;
    
    private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();
    private final ObservableList<ResourceInfo> resourceData = FXCollections.observableArrayList();
    private final ObservableList<FileSystemInfo> fileSystemData = FXCollections.observableArrayList();
    private final ObservableList<ResourceInfo> cpuCoreData = FXCollections.observableArrayList();
    private final ObservableList<StartupInfo> startupData = FXCollections.observableArrayList();
    private TreeItem<StartupGroup> startupTreeRoot;
    
        // Constructor demonstrating Dependency Injection
    public SystemInfoTable() {
        this.processService = new ProcessService();
        this.resourceService = new ResourceMonitoringService();
        this.fileSystemService = new FileSystemService();
        this.startupService = new StartupService();
    }
    
    private Timeline refreshTimeline;
    
    private LineChart<String, Number> cpuLineChart;
    private LineChart<String, Number> memoryLineChart;
    private LineChart<String, Number> swapLineChart;
    private TableView<ResourceInfo> cpuTableView;

    // Line chart series for historical data
    private final XYChart.Series<String, Number> cpuHistorySeries = new XYChart.Series<>();
    private final XYChart.Series<String, Number> memoryHistorySeries = new XYChart.Series<>(); 
    private final XYChart.Series<String, Number> swapHistorySeries = new XYChart.Series<>();
    private static final int MAX_DATA_POINTS = 30; // Keep last 30 data points

    private TableView<ProcessInfo> processTable;
    
    // Dashboard cards for modern UI
    private DashboardCard cpuCard;
    private DashboardCard memoryCard;
    private DashboardCard swapCard;
    private DashboardCard diskCard;

    // --- PERFORMANCE OPTIMIZATION PATCH START ---
    // Updated to use ResourceMonitoringService - demonstrates Service Layer pattern  
    private List<ResourceInfo> getSystemResources() {
        return resourceService.getSystemResources();
    }
    // ... existing code ...
    // 2. Increase refresh interval for main data (was 1s, now 3s)
    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            refreshProcessData();
            refreshResourceData();
            refreshFileSystemData();
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        // Keep startup data refresh at 60 seconds
        Timeline startupRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> {
            refreshStartupData();
        }));
        startupRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        startupRefreshTimeline.play();
    }
    // 3. Updated to use FileSystemService and StartupService - demonstrates Service Layer pattern
    private List<FileSystemInfo> getFileSystemInfo() {
        return fileSystemService.getFileSystemInfo();
    }
    
    private List<StartupInfo> getStartupApplications() {
        return startupService.getStartupApplications();
    }
    // 4. Only update changed process/resource data in refreshProcessData/refreshResourceData
    private void refreshProcessData() {
        Task<List<ProcessInfo>> task = new Task<List<ProcessInfo>>() {
            @Override
            protected List<ProcessInfo> call() throws Exception {
                return getProcessInfoFromOSHI();
            }
        };
        task.setOnSucceeded(e -> {
            List<ProcessInfo> newData = task.getValue();
            Platform.runLater(() -> {
                if (!processData.equals(newData)) {
                    processData.setAll(newData);
                }
                // Save currently selected process PID if any
                ProcessInfo selectedProcess = processTable != null ? processTable.getSelectionModel().getSelectedItem() : null;
                String selectedPid = selectedProcess != null ? selectedProcess.getPid() : null;
                
                // Save current sort order
                List<TableColumn<ProcessInfo, ?>> currentSortOrder = new ArrayList<>();
                List<TableColumn.SortType> currentSortTypes = new ArrayList<>();
                if (processTable != null && !processTable.getSortOrder().isEmpty()) {
                    for (TableColumn<ProcessInfo, ?> col : processTable.getSortOrder()) {
                        currentSortOrder.add(col);
                        currentSortTypes.add(col.getSortType());
                    }
                }
                
                // Temporarily disable selection events during update
                processTable.getSelectionModel().clearSelection();
                
                // Restore sort order
                if (processTable != null) {
                    if (!currentSortOrder.isEmpty()) {
                        processTable.getSortOrder().clear();
                        processTable.getSortOrder().addAll(currentSortOrder);
                        for (int i = 0; i < currentSortOrder.size(); i++) {
                            currentSortOrder.get(i).setSortType(currentSortTypes.get(i));
                        }
                    } else {
                        // Default to CPU sort if no previous sort
                        processTable.getSortOrder().clear();
                        TableColumn<ProcessInfo, ?> cpuColumn = processTable.getColumns().stream()
                            .filter(col -> "CPU (%)".equals(col.getText())).findFirst().orElse(null);
                        if (cpuColumn != null) {
                            processTable.getSortOrder().add(cpuColumn);
                            cpuColumn.setSortType(TableColumn.SortType.DESCENDING);
                        }
                    }
                    processTable.sort();
                    
                    // Clear any automatic selection first
                    processTable.getSelectionModel().clearSelection();
                    
                    // Restore selection only if the process still exists (without affecting sort order)
                    if (selectedPid != null) {
                        // Find the process in the current sorted list
                        for (int i = 0; i < processData.size(); i++) {
                            ProcessInfo process = processData.get(i);
                            if (selectedPid.equals(process.getPid())) {
                                // Select by index to maintain sort order - NO SCROLLING
                                processTable.getSelectionModel().select(i);
                                break;
                            }
                        }
                    }
                }
            });
        });
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh process data: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }
    private void refreshResourceData() {
        Task<List<ResourceInfo>> task = new Task<List<ResourceInfo>>() {
            @Override
            protected List<ResourceInfo> call() throws Exception {
                return getSystemResources();
            }
        };
        task.setOnSucceeded(e -> {
            List<ResourceInfo> newData = task.getValue();
            Platform.runLater(() -> {
                if (!resourceData.equals(newData)) {
                    resourceData.setAll(newData);
                }
                // Update charts if they exist
                if (cpuLineChart != null && memoryLineChart != null && swapLineChart != null) {
                    updateCharts(newData);
                }
                
                // Update dashboard cards
                updateDashboardCards(newData);
            });
        });
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh resource data: " + task.getException().getMessage());
        });
        new Thread(task).start();
    }
    // --- PERFORMANCE OPTIMIZATION PATCH END ---

    // Updated to use ProcessService - demonstrates Service Layer pattern
    private List<ProcessInfo> getProcessInfoFromOSHI() {
        return processService.getTopProcesses();
    }

    private void updateCharts(List<ResourceInfo> resources) {
        // Calculate overall CPU usage as average of all cores
        double totalCpuUsage = 0.0;
        int cpuCoreCount = 0;
        double memoryUsagePercent = 0.0;
        double swapUsagePercent = 0.0;
        List<ResourceInfo> cpuCores = new ArrayList<>();
        
        for (ResourceInfo resource : resources) {
            if (resource.getName().startsWith("CPU Core")) {
                totalCpuUsage += resource.getUsedPercent();
                cpuCoreCount++;
                cpuCores.add(resource);
            } else if (resource.getName().equals("Memory")) {
                memoryUsagePercent = resource.getUsedPercent();
            } else if (resource.getName().equals("Swap")) {
                swapUsagePercent = resource.getUsedPercent();
            }
        }
        
        // Calculate average CPU usage
        if (cpuCoreCount > 0) {
            totalCpuUsage = totalCpuUsage / cpuCoreCount;
        }
        
        // Update CPU core table
        Platform.runLater(() -> {
            cpuCoreData.clear();
            cpuCoreData.addAll(cpuCores);
            // Also update with latest CPU core data from service
            List<ResourceInfo> latestCpuCores = resourceService.getCpuCoreData();
            cpuCoreData.clear();
            cpuCoreData.addAll(latestCpuCores);
        });
        
        // Add data points to line charts with timestamp
        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm:ss");
        String timeLabel = format.format(new java.util.Date(now));
        
        // Update CPU line chart
        cpuHistorySeries.getData().add(new XYChart.Data<>(timeLabel, totalCpuUsage));
        if (cpuHistorySeries.getData().size() > MAX_DATA_POINTS) {
            cpuHistorySeries.getData().remove(0);
        }
        
        // Update Memory line chart
        memoryHistorySeries.getData().add(new XYChart.Data<>(timeLabel, memoryUsagePercent));
        if (memoryHistorySeries.getData().size() > MAX_DATA_POINTS) {
            memoryHistorySeries.getData().remove(0);
        }
        
        // Update Swap line chart
        swapHistorySeries.getData().add(new XYChart.Data<>(timeLabel, swapUsagePercent));
        if (swapHistorySeries.getData().size() > MAX_DATA_POINTS) {
            swapHistorySeries.getData().remove(0);
        }
    }
    
    private void refreshFileSystemData() {
        Task<List<FileSystemInfo>> task = new Task<List<FileSystemInfo>>() {
            @Override
            protected List<FileSystemInfo> call() throws Exception {
                return getFileSystemInfo();
            }
        };
        
        task.setOnSucceeded(e -> {
            List<FileSystemInfo> newData = task.getValue();
            Platform.runLater(() -> {
                fileSystemData.clear();
                fileSystemData.addAll(newData);
            });
        });
        
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh file system data: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
    }
    
    private TreeTableView<StartupGroup> createStartupTreeTable() {
        // Initialize root
        startupTreeRoot = new TreeItem<>(new StartupGroup("Root"));
        startupTreeRoot.setExpanded(true);
        
        TreeTableView<StartupGroup> treeTable = new TreeTableView<>(startupTreeRoot);
        treeTable.setShowRoot(false);
        
        // Name column
        TreeTableColumn<StartupGroup, String> nameColumn = new TreeTableColumn<>("Name");
        nameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getValue().getDisplayName()));
        nameColumn.setPrefWidth(350);
        
        // Path column  
        TreeTableColumn<StartupGroup, String> pathColumn = new TreeTableColumn<>("Path/Command");
        pathColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getValue().getDisplayPath()));
        pathColumn.setPrefWidth(450);
        
        treeTable.getColumns().add(nameColumn);
        treeTable.getColumns().add(pathColumn);
        
        return treeTable;
    }
    
    private void updateStartupTreeData() {
        Platform.runLater(() -> {
            // Clear existing children
            startupTreeRoot.getChildren().clear();
            
            // Group startup items by directory
            Map<String, List<StartupInfo>> directoryGroups = new HashMap<>();
            
            for (StartupInfo item : startupData) {
                String directory = item.getDirectory();
                directoryGroups.computeIfAbsent(directory, k -> new ArrayList<>()).add(item);
            }
            
            // Create tree structure
            for (Map.Entry<String, List<StartupInfo>> entry : directoryGroups.entrySet()) {
                String directory = entry.getKey();
                List<StartupInfo> items = entry.getValue();
                
                if (items.size() == 1) {
                    // Single item - add directly
                    StartupGroup singleItemGroup = new StartupGroup(items.get(0));
                    TreeItem<StartupGroup> itemNode = new TreeItem<>(singleItemGroup);
                    startupTreeRoot.getChildren().add(itemNode);
                } else {
                    // Multiple items - create directory group
                    StartupGroup directoryGroup = new StartupGroup(directory);
                    TreeItem<StartupGroup> directoryNode = new TreeItem<>(directoryGroup);
                    directoryNode.setExpanded(false); // Initially collapsed
                    
                    // Add individual items under directory
                    for (StartupInfo item : items) {
                        directoryGroup.addItem(item);
                        StartupGroup itemGroup = new StartupGroup(item);
                        TreeItem<StartupGroup> itemNode = new TreeItem<>(itemGroup);
                        directoryNode.getChildren().add(itemNode);
                    }
                    
                    startupTreeRoot.getChildren().add(directoryNode);
                }
            }
        });
    }

    private void refreshStartupData() {
        Task<List<StartupInfo>> task = new Task<List<StartupInfo>>() {
            @Override
            protected List<StartupInfo> call() {
                return getStartupApplications();
            }
        };

        task.setOnSucceeded(e -> {
            List<StartupInfo> newData = task.getValue();
            Platform.runLater(() -> {
                startupData.clear();
                startupData.addAll(newData);
                // Update tree view data
                if (startupTreeRoot != null) {
                    updateStartupTreeData();
                }
            });
        });

        task.setOnFailed(e -> {
            System.err.println("Failed to refresh startup data: " + e.getSource().getException());
             if (e.getSource().getException() != null) {
                e.getSource().getException().printStackTrace();
            }
        });

        new Thread(task).start();
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize modern UI
        ModernUIManager.initialize();
        ModernUIManager.setupStage(primaryStage);

        // Create modern top bar
        ModernTopBar topBar = new ModernTopBar(
            this::manualRefresh,
            this::toggleAutoRefresh
        );

        // Create main content with modern styling
        TabPane tabPane = createModernTabPane();
        
        Tab processTab = new Tab("Processes");
        processTab.setGraphic(ModernUIManager.Icons.process());
        processTable = new TableView<>(processData); // Sử dụng biến instance
        TableColumn<ProcessInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<ProcessInfo, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("user"));
        TableColumn<ProcessInfo, String> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));
        TableColumn<ProcessInfo, Double> cpuCol = new TableColumn<>("CPU (%)");
        cpuCol.setCellValueFactory(new PropertyValueFactory<>("cpuValue"));
        cpuCol.setCellFactory(column -> new javafx.scene.control.TableCell<ProcessInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", item));
                }
            }
        });
        TableColumn<ProcessInfo, String> rssCol = new TableColumn<>("RSS (MB)");
        rssCol.setCellValueFactory(new PropertyValueFactory<>("rss"));
        TableColumn<ProcessInfo, String> vszCol = new TableColumn<>("VSZ (MB)");
        vszCol.setCellValueFactory(new PropertyValueFactory<>("virtualMem"));
        TableColumn<ProcessInfo, String> diskReadCol = new TableColumn<>("Disk Read (MB)");
        diskReadCol.setCellValueFactory(new PropertyValueFactory<>("diskRead"));
        
        // Thiết lập sắp xếp mặc định theo CPU giảm dần
        processTable.getColumns().addAll(nameCol, userCol, pidCol, cpuCol, rssCol, vszCol, diskReadCol);
        processTable.getSortOrder().add(cpuCol);
        cpuCol.setSortType(TableColumn.SortType.DESCENDING);
        processTable.sort();
        
        // Clear any automatic selection after initial setup
        processTable.getSelectionModel().clearSelection();
        
        // Đảm bảo luôn giữ sắp xếp theo CPU nếu người dùng bỏ sort
        processTable.setOnSort(event -> {
            if (processTable.getSortOrder().isEmpty()) {
                processTable.getSortOrder().add(cpuCol);
                cpuCol.setSortType(TableColumn.SortType.DESCENDING);
            }
        });
        
        // Add context menu for End Process functionality
        ContextMenu contextMenu = new ContextMenu();
        MenuItem endProcessItem = new MenuItem("End Process");
        endProcessItem.setOnAction(event -> {
            ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
            if (selectedProcess != null) {
                // Show confirmation dialog using service
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Process Termination");
                confirmAlert.setHeaderText("End Process");
                confirmAlert.setContentText(processService.getTerminationConfirmMessage(selectedProcess));
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Run kill process in background thread using service
                        Task<Void> killTask = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                boolean success = processService.terminateProcess(selectedProcess);
                                Platform.runLater(() -> {
                                    Alert alert;
                                    if (success) {
                                        alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Process Terminated");
                                        alert.setHeaderText("Success");
                                        alert.setContentText("Process has been terminated successfully.");
                                    } else {
                                        alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("Process Termination Failed");
                                        alert.setHeaderText("Error");
                                        alert.setContentText("Failed to terminate process. You may not have sufficient permissions.");
                                    }
                                    alert.showAndWait();
                                    refreshProcessData();
                                });
                                return null;
                            }
                        };
                        new Thread(killTask).start();
                    }
                });
            }
        });
        contextMenu.getItems().add(endProcessItem);
        processTable.setContextMenu(contextMenu);
        
        // Add End Process button below the table
        Button endProcessButton = new Button("End Selected Process");
        endProcessButton.setGraphic(ModernUIManager.Icons.stop());
        endProcessButton.getStyleClass().addAll("button", "danger");
        endProcessButton.setOnAction(event -> {
            ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
            if (selectedProcess != null) {
                // Show confirmation dialog using service
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Process Termination");
                confirmAlert.setHeaderText("End Process");
                confirmAlert.setContentText(processService.getTerminationConfirmMessage(selectedProcess));
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Run kill process in background thread using service
                        Task<Void> killTask = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                boolean success = processService.terminateProcess(selectedProcess);
                                Platform.runLater(() -> {
                                    Alert alert;
                                    if (success) {
                                        alert = new Alert(Alert.AlertType.INFORMATION);
                                        alert.setTitle("Process Terminated");
                                        alert.setHeaderText("Success");
                                        alert.setContentText("Process has been terminated successfully.");
                                    } else {
                                        alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("Process Termination Failed");
                                        alert.setHeaderText("Error");
                                        alert.setContentText("Failed to terminate process. You may not have sufficient permissions.");
                                    }
                                    alert.showAndWait();
                                    refreshProcessData();
                                });
                                return null;
                            }
                        };
                        new Thread(killTask).start();
                    }
                });
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Process Selected");
                alert.setHeaderText("Warning");
                alert.setContentText("Please select a process from the table to terminate.");
                alert.showAndWait();
            }
        });
        
        VBox processLayout = new VBox(10, processTable, endProcessButton);
        processLayout.setPadding(new Insets(20));
        processLayout.setAlignment(Pos.CENTER);
        ModernUIManager.applyCardStyle(processLayout);
        processTab.setContent(processLayout);
        
        Tab resourceTab = new Tab("Resources");
        resourceTab.setGraphic(ModernUIManager.Icons.cpu());
        VBox resourceLayout = new VBox(10); // Main container for resources tab
        resourceLayout.setPadding(new Insets(20));
        resourceLayout.setAlignment(Pos.TOP_CENTER); // Align content to top center
        ModernUIManager.applyCardStyle(resourceLayout);

        // Create and add charts
        VBox resourceChartsContainer = createResourceCharts();
        
        // Placeholder for the old TableView<ResourceInfo> if needed, or remove if charts replace it entirely
        // TableView<ResourceInfo> resourcesTable = new TableView<>(resourceData); 
        // ... (setup columns for resourcesTable if you keep it)
        // resourceLayout.getChildren().addAll(resourceChartsContainer, resourcesTable); // If keeping table
        resourceLayout.getChildren().add(resourceChartsContainer); // If only charts

        resourceTab.setContent(resourceLayout);
        
        Tab fileSystemTab = new Tab("File System");
        fileSystemTab.setGraphic(ModernUIManager.Icons.storage());
        TableView<FileSystemInfo> fileSystemTable = new TableView<>(fileSystemData);
        TableColumn<FileSystemInfo, String> mountCol = new TableColumn<>("Mount Point");
        mountCol.setCellValueFactory(new PropertyValueFactory<>("mountPoint"));
        TableColumn<FileSystemInfo, String> nameColFS = new TableColumn<>("Name");
        nameColFS.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<FileSystemInfo, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        TableColumn<FileSystemInfo, String> totalCol = new TableColumn<>("Total Space (GB)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalSpace"));
        TableColumn<FileSystemInfo, String> usedCol = new TableColumn<>("Used Space (GB)");
        usedCol.setCellValueFactory(new PropertyValueFactory<>("usedSpace"));
        TableColumn<FileSystemInfo, String> availableCol = new TableColumn<>("Available Space (GB)");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("usableSpace"));
        
        fileSystemTable.getColumns().addAll(mountCol, nameColFS, typeCol, totalCol, usedCol, availableCol);
        
        VBox fileSystemLayout = new VBox(fileSystemTable);
        fileSystemLayout.setPadding(new Insets(20));
        ModernUIManager.applyCardStyle(fileSystemLayout);
        fileSystemTab.setContent(fileSystemLayout);
        
        Tab startupTab = new Tab("Startup");
        startupTab.setGraphic(ModernUIManager.Icons.startup());
        TreeTableView<StartupGroup> startupTreeTable = createStartupTreeTable();
        
        VBox startupLayout = new VBox(startupTreeTable);
        startupLayout.setPadding(new Insets(20));
        ModernUIManager.applyCardStyle(startupLayout);
        startupTab.setContent(startupLayout);
        
        tabPane.getTabs().addAll(processTab, resourceTab, fileSystemTab, startupTab);

        // Create main layout with top bar
        VBox mainLayout = new VBox();
        mainLayout.getChildren().addAll(topBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 1200, 800);
        ModernUIManager.setupScene(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        refreshStartupData();
        startAutoRefresh();
    }

    private TabPane createModernTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("modern-tab-pane");
        return tabPane;
    }

    private void manualRefresh() {
        refreshProcessData();
        refreshResourceData();
        refreshFileSystemData();
    }

    private void toggleAutoRefresh() {
        if (refreshTimeline != null) {
            if (refreshTimeline.getStatus() == Timeline.Status.RUNNING) {
                refreshTimeline.stop();
            } else {
                refreshTimeline.play();
            }
        }
    }

    private VBox createResourceCharts() {
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setAlignment(Pos.TOP_CENTER);

        // Create modern dashboard cards
        HBox dashboardCards = createDashboardCards();
        
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);

        // CPU Usage Line Chart
        CategoryAxis cpuXAxis = new CategoryAxis();
        cpuXAxis.setLabel("Time");
        cpuXAxis.setTickLabelsVisible(false);
        NumberAxis cpuYAxis = new NumberAxis(0, 100, 10);
        cpuYAxis.setLabel("CPU Usage (%)");
        cpuLineChart = new LineChart<>(cpuXAxis, cpuYAxis);
        cpuLineChart.setTitle("CPU Usage History");
        cpuLineChart.setAnimated(false);
        cpuLineChart.setCreateSymbols(false);
        cpuLineChart.setPrefHeight(250);
        cpuLineChart.setPrefWidth(300);
        
        cpuHistorySeries.setName("CPU Usage");
        cpuLineChart.getData().add(cpuHistorySeries);

        // Memory Usage Line Chart
        CategoryAxis memoryXAxis = new CategoryAxis();
        memoryXAxis.setLabel("Time");
        memoryXAxis.setTickLabelsVisible(false);
        NumberAxis memoryYAxis = new NumberAxis(0, 100, 10);
        memoryYAxis.setLabel("Memory Usage (%)");
        memoryLineChart = new LineChart<>(memoryXAxis, memoryYAxis);
        memoryLineChart.setTitle("Memory Usage History");
        memoryLineChart.setAnimated(false);
        memoryLineChart.setCreateSymbols(false);
        memoryLineChart.setPrefHeight(250);
        memoryLineChart.setPrefWidth(300);
        
        memoryHistorySeries.setName("Memory Usage");
        memoryLineChart.getData().add(memoryHistorySeries);

        // Swap Usage Line Chart
        CategoryAxis swapXAxis = new CategoryAxis();
        swapXAxis.setLabel("Time");
        swapXAxis.setTickLabelsVisible(false);
        NumberAxis swapYAxis = new NumberAxis(0, 100, 10);
        swapYAxis.setLabel("Swap Usage (%)");
        swapLineChart = new LineChart<>(swapXAxis, swapYAxis);
        swapLineChart.setTitle("Swap Usage History");
        swapLineChart.setAnimated(false);
        swapLineChart.setCreateSymbols(false);
        swapLineChart.setPrefHeight(250);
        swapLineChart.setPrefWidth(300);
        
        swapHistorySeries.setName("Swap Usage");
        swapLineChart.getData().add(swapHistorySeries);

        cpuTableView = new TableView<>();
        cpuTableView.setItems(cpuCoreData);
        cpuTableView.setPrefHeight(200);
        cpuTableView.getStyleClass().add("cpu-core-table");

        TableColumn<ResourceInfo, String> coreNameCol = new TableColumn<>("Core");
        coreNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        coreNameCol.setPrefWidth(120);

        TableColumn<ResourceInfo, Double> usageCol = new TableColumn<>("Usage (%)");
        usageCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getUsedPercent()).asObject());
        usageCol.setPrefWidth(160);
        usageCol.setCellFactory(col -> new javafx.scene.control.TableCell<ResourceInfo, Double>() {
            private final javafx.scene.control.ProgressBar bar = new javafx.scene.control.ProgressBar();
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    double percent = value / 100.0;
                    bar.setProgress(percent);
                    bar.setPrefWidth(100);
                    // Color code: green <40, yellow <70, orange <90, red >=90
                    String color;
                    if (value < 40) color = "#4caf50"; // green
                    else if (value < 70) color = "#ffeb3b"; // yellow
                    else if (value < 90) color = "#ff9800"; // orange
                    else color = "#f44336"; // red
                    bar.setStyle("-fx-accent: " + color + ";");
                    setGraphic(bar);
                    setText(String.format("%.2f%%", value));
                }
            }
        });

        TableColumn<ResourceInfo, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> new javafx.scene.control.TableCell<ResourceInfo, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    String bg;
                    switch (status) {
                        case "Low": bg = "#4caf50"; break; // green
                        case "Medium": bg = "#ffeb3b"; break; // yellow
                        case "High": bg = "#f44336"; break; // red
                        default: bg = "#ffffff"; break;
                    }
                    setStyle("-fx-background-color: " + bg + "; -fx-text-fill: black;");
                }
            }
        });
        cpuTableView.getColumns().addAll(coreNameCol, usageCol, statusCol);

        // Add to grid: row 0 (charts)
        gridPane.add(cpuLineChart, 0, 0);       // CPU Chart
        gridPane.add(memoryLineChart, 1, 0);    // Memory Chart
        gridPane.add(swapLineChart, 2, 0);      // Swap Chart
        // Add to grid: row 1 (table)
        gridPane.add(cpuTableView, 0, 1, 3, 1); // Table spans all 3 columns

        // Set column constraints for 3 equal columns
        for (int i = 0; i < 3; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(33.33);
            gridPane.getColumnConstraints().add(col);
        }

        mainContainer.getChildren().addAll(dashboardCards, gridPane);
        return mainContainer;
    }

    private HBox createDashboardCards() {
        HBox cardsContainer = new HBox(15);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPadding(new Insets(10));

        // Create dashboard cards
        cpuCard = new DashboardCard("CPU Usage", ModernUIManager.Icons.cpu());
        memoryCard = new DashboardCard("Memory Usage", ModernUIManager.Icons.memory());
        swapCard = new DashboardCard("Swap Usage", ModernUIManager.Icons.storage());
        diskCard = new DashboardCard("Disk I/O", ModernUIManager.Icons.storage());

        cardsContainer.getChildren().addAll(cpuCard, memoryCard, swapCard, diskCard);
        return cardsContainer;
    }

    private void updateDashboardCards(List<ResourceInfo> resources) {
        if (resources == null || resources.isEmpty()) return;

        for (ResourceInfo resource : resources) {
            String name = resource.getName();
            double usedPercent = resource.getUsedPercent();
            
            if (name.contains("CPU") || name.contains("Processor")) {
                cpuCard.updateValue(String.format("%.1f", usedPercent), "%");
                cpuCard.updateProgress(usedPercent / 100.0);
            } else if (name.contains("Memory") || name.contains("RAM")) {
                memoryCard.updateValue(String.format("%.1f", usedPercent), "%");
                memoryCard.updateProgress(usedPercent / 100.0);
            } else if (name.contains("Swap")) {
                swapCard.updateValue(String.format("%.1f", usedPercent), "%");
                swapCard.updateProgress(usedPercent / 100.0);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}