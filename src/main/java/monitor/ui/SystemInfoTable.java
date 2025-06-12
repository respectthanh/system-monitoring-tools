package monitor.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos; // Ensured import
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class SystemInfoTable extends Application {
    private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();
    private final ObservableList<ResourceInfo> resourceData = FXCollections.observableArrayList();
    private final ObservableList<FileSystemInfo> fileSystemData = FXCollections.observableArrayList();
    private final ObservableList<ResourceInfo> cpuCoreData = FXCollections.observableArrayList();
    private final ObservableList<StartupInfo> startupData = FXCollections.observableArrayList();
    
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

    private final Map<Integer, OSProcess> previousProcessMap = new HashMap<>();
    private long previousTimestamp;
    private TableView<ProcessInfo> processTable; // Thêm biến instance

    // Inner class definitions (ensured they are present)
    public static class ProcessInfo {
        // ... existing ProcessInfo code ...
        private final String name;
        private final String user;
        private final String pid;
        private final String cpu;
        private final Double cpuValue;
        private final String rss;
        private final Double rssValue; 
        private final String virtualMem;
        private final Double virtualMemValue;
        private final String diskRead;
        private final Double diskReadValue;

        public ProcessInfo(String name, String user, String pid, 
                          double cpuValue, double rssValue, 
                          double virtualMemValue, double diskReadValue) {
            this.name = name;
            this.user = user;
            this.pid = pid;
            this.cpuValue = cpuValue;
            this.cpu = String.format("%.2f", cpuValue);
            this.rssValue = rssValue;
            this.rss = String.format("%.2f", rssValue);
            this.virtualMemValue = virtualMemValue;
            this.virtualMem = String.format("%.2f", virtualMemValue);
            this.diskReadValue = diskReadValue;
            this.diskRead = String.format("%.2f", diskReadValue);
        }

        public String getName() { return name; }
        public String getUser() { return user; }
        public String getPid() { return pid; }
        public String getCpu() { return cpu; }
        public Double getCpuValue() { return cpuValue; }
        public String getRss() { return rss; }
        public Double getRssValue() { return rssValue; }
        public String getVirtualMem() { return virtualMem; }
        public Double getVirtualMemValue() { return virtualMemValue; }
        public String getDiskRead() { return diskRead; }
        public Double getDiskReadValue() { return diskReadValue; }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ProcessInfo that = (ProcessInfo) obj;
            return pid.equals(that.pid);
        }
        
        @Override
        public int hashCode() {
            return pid.hashCode();
        }
    }

    public static class ResourceInfo {
        private final String name;
        private final String status;
        private final String used;
        private final String total;
        private final double usedPercent; 

        public ResourceInfo(String name, String status, String used, String total, double usedPercent) {
            this.name = name;
            this.status = status;
            this.used = used;
            this.total = total;
            this.usedPercent = usedPercent;
        }

        public String getName() { return name; }
        public String getStatus() { return status; }
        public String getUsed() { return used; }
        public String getTotal() { return total; }
        public double getUsedPercent() { return usedPercent; }
    }

    public static class FileSystemInfo {
        private final String mountPoint;
        private final String name;
        private final String type;
        private final String totalSpace;
        private final String usedSpace;
        private final String usableSpace;

        public FileSystemInfo(String mountPoint, String name, String type, long totalSpace, long usedSpace, long usableSpace) {
            this.mountPoint = mountPoint;
            this.name = name;
            this.type = type;
            this.totalSpace = String.format("%.2f GB", totalSpace / (1024.0 * 1024 * 1024));
            this.usedSpace = String.format("%.2f GB", usedSpace / (1024.0 * 1024 * 1024));
            this.usableSpace = String.format("%.2f GB", usableSpace / (1024.0 * 1024 * 1024));
        }

        public String getMountPoint() { return mountPoint; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getTotalSpace() { return totalSpace; }
        public String getUsedSpace() { return usedSpace; }
        public String getUsableSpace() { return usableSpace; }
    }
    
    public static class StartupInfo {
        private final String name;
        private final String path;

        public StartupInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
    }

    // ... existing getProcessInfoFromOSHI, getStartupApplications, getStartupAppsFromFolder ...
    private List<ProcessInfo> getProcessInfoFromOSHI() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        HardwareAbstractionLayer hardware = si.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        int logicalProcessorCount = processor.getLogicalProcessorCount();

        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 0);
        long currentTimestamp = System.currentTimeMillis();
        
        List<ProcessInfo> result = new ArrayList<>();

        for (OSProcess p : processes) {
            double cpu = 0.0;
            if (previousProcessMap.containsKey(p.getProcessID()) && previousTimestamp > 0) {
                OSProcess old = previousProcessMap.get(p.getProcessID());
                long elapsed = currentTimestamp - previousTimestamp;
                if (elapsed > 0) {
                    long cputime = p.getKernelTime() + p.getUserTime();
                    long oldcputime = old.getKernelTime() + old.getUserTime();
                    cpu = ((cputime - oldcputime) * 100.0 / elapsed) / logicalProcessorCount;
                }
            }
            
            double rssMB = p.getResidentSetSize() / (1024.0 * 1024);
            double virtualMemMB = p.getVirtualSize() / (1024.0 * 1024);
            double diskReadMB = p.getBytesRead() / (1024.0 * 1024);
        
            result.add(new ProcessInfo(
                p.getName(),
                p.getUser(),
                String.valueOf(p.getProcessID()),
                Math.max(0.0, cpu),
                rssMB,
                virtualMemMB,
                diskReadMB
            ));
        }
        
        previousProcessMap.clear();
        for (OSProcess p : processes) {
            previousProcessMap.put(p.getProcessID(), p);
        }
        previousTimestamp = currentTimestamp;
        
        return result;
    }

    private List<StartupInfo> getStartupApplications() {
        List<StartupInfo> startupApps = new ArrayList<>();
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            String userStartupFolder = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
            String allUsersStartupFolder = System.getenv("PROGRAMDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
            startupApps.addAll(getStartupAppsFromFolder(userStartupFolder));
            startupApps.addAll(getStartupAppsFromFolder(allUsersStartupFolder));
        } else if (osName.contains("linux")) {
            String userAutostartFolder = System.getProperty("user.home") + "/.config/autostart";
            String systemAutostartFolder = "/etc/xdg/autostart";
            startupApps.addAll(getStartupAppsFromFolder(userAutostartFolder));
            startupApps.addAll(getStartupAppsFromFolder(systemAutostartFolder));
            
            SystemdStartupDetector systemdDetector = new SystemdStartupDetector();
            startupApps.addAll(systemdDetector.getSystemdStartupServices());
            
            startupApps.addAll(systemdDetector.getCronJobsAtReboot());
            
            startupApps.addAll(systemdDetector.getRcLocalEntries());
        }

        return startupApps;
    }

    private List<StartupInfo> getStartupAppsFromFolder(String folderPath) {
        List<StartupInfo> apps = new ArrayList<>();
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".desktop") || name.endsWith(".lnk"));
            if (files != null) {
                for (File file : files) {
                    apps.add(new StartupInfo(file.getName(), file.getAbsolutePath()));
                }
            }
        }
        return apps;
    }

    private List<ResourceInfo> getSystemResources() {
        List<ResourceInfo> resources = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hardware = si.getHardware();
        
        CentralProcessor processor = hardware.getProcessor();
        long[][] prevProcTicks = processor.getProcessorCpuLoadTicks();
        // Wait a second...
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return resources; // Or handle error appropriately
        }
        double[] cpuLoads = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
        
        for (int i = 0; i < cpuLoads.length; i++) {
            double coreLoad = cpuLoads[i] * 100;
            resources.add(new ResourceInfo(
                String.format("CPU Core %d", i), 
                String.format("%.2f%%", coreLoad), 
                coreLoad > 50 ? "High" : coreLoad > 20 ? "Medium" : "Low", 
                "100%", 
                coreLoad
            ));
        }
        
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double)usedMemory / totalMemory * 100.0;
        resources.add(new ResourceInfo(
            "Memory",
            String.format("%.2f%%", memoryUsagePercent),
            String.format("%.2f GB", usedMemory / 1e9),
            String.format("%.2f GB", totalMemory / 1e9),
            memoryUsagePercent
        ));
        
        long totalSwap = memory.getVirtualMemory().getSwapTotal();
        long usedSwap = memory.getVirtualMemory().getSwapUsed();
        double swapUsagePercent = totalSwap > 0 ? (double)usedSwap / totalSwap * 100.0 : 0.0;
        resources.add(new ResourceInfo(
            "Swap",
            String.format("%.2f%%", swapUsagePercent),
            String.format("%.2f GB", usedSwap / 1e9),
            String.format("%.2f GB", totalSwap / 1e9),
            swapUsagePercent
        ));
        
        return resources;
    }
    
    private List<FileSystemInfo> getFileSystemInfo() {
        List<FileSystemInfo> filesystems = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
        
        for (OSFileStore fs : fileSystem.getFileStores()) {
            String mountPoint = fs.getMount();
            String name = fs.getName();
            String type = fs.getType();
            long totalSpace = fs.getTotalSpace();
            long usableSpace = fs.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            filesystems.add(new FileSystemInfo(
                mountPoint,
                name,
                type,
                totalSpace,
                usedSpace,
                usableSpace
            ));
        }
        
        return filesystems;
    }
    
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
                
                // Update data
                processData.clear();
                processData.addAll(newData);
                
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
                        processTable.getSortOrder().add(processTable.getColumns().stream()
                            .filter(col -> "CPU (%)".equals(col.getText())).findFirst().orElse(null));
                        processTable.getSortOrder().get(0).setSortType(TableColumn.SortType.DESCENDING);
                    }
                    processTable.sort();
                    
                    // Restore selection if the process still exists
                    if (selectedPid != null) {
                        for (ProcessInfo process : processData) {
                            if (selectedPid.equals(process.getPid())) {
                                processTable.getSelectionModel().select(process);
                                processTable.scrollTo(process);
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
                resourceData.clear();
                resourceData.addAll(newData);
                
                // Update charts if they exist
                if (cpuLineChart != null && memoryLineChart != null && swapLineChart != null) {
                    updateCharts(newData);
                }
            });
        });
        
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh resource data: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
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
    
    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> { // Ensure this is 1 second
            refreshProcessData();
            refreshResourceData(); 
            refreshFileSystemData();
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private VBox createResourceCharts() {
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

        VBox container = new VBox(gridPane);
        container.setAlignment(Pos.CENTER);
        return container;
    }
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("System Monitoring Tool");

        TabPane tabPane = new TabPane();
        
        Tab processTab = new Tab("Processes");
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
                // Show confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Process Termination");
                confirmAlert.setHeaderText("End Process");
                confirmAlert.setContentText("Are you sure you want to terminate process '" 
                    + selectedProcess.getName() + "' (PID: " + selectedProcess.getPid() + ")?");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Run kill process in background thread
                        Task<Void> killTask = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                killProcess(selectedProcess.getPid());
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
        endProcessButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        endProcessButton.setOnAction(event -> {
            ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
            if (selectedProcess != null) {
                // Show confirmation dialog
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Process Termination");
                confirmAlert.setHeaderText("End Process");
                confirmAlert.setContentText("Are you sure you want to terminate process '" 
                    + selectedProcess.getName() + "' (PID: " + selectedProcess.getPid() + ")?");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Run kill process in background thread
                        Task<Void> killTask = new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                killProcess(selectedProcess.getPid());
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
        processLayout.setPadding(new Insets(10));
        processLayout.setAlignment(Pos.CENTER);
        processTab.setContent(processLayout);
        
        Tab resourceTab = new Tab("Resources");
        VBox resourceLayout = new VBox(10); // Main container for resources tab
        resourceLayout.setPadding(new Insets(10));
        resourceLayout.setAlignment(Pos.TOP_CENTER); // Align content to top center

        // Create and add charts
        VBox resourceChartsContainer = createResourceCharts();
        
        // Placeholder for the old TableView<ResourceInfo> if needed, or remove if charts replace it entirely
        // TableView<ResourceInfo> resourcesTable = new TableView<>(resourceData); 
        // ... (setup columns for resourcesTable if you keep it)
        // resourceLayout.getChildren().addAll(resourceChartsContainer, resourcesTable); // If keeping table
        resourceLayout.getChildren().add(resourceChartsContainer); // If only charts

        resourceTab.setContent(resourceLayout);
        
        Tab fileSystemTab = new Tab("File System");
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
        fileSystemTab.setContent(fileSystemTable);
        
        Tab startupTab = new Tab("Startup");
        TableView<StartupInfo> startupTable = new TableView<>(startupData);
        TableColumn<StartupInfo, String> startupNameCol = new TableColumn<>("Name");
        startupNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        startupNameCol.setPrefWidth(250);

        TableColumn<StartupInfo, String> startupPathCol = new TableColumn<>("Path/Command");
        startupPathCol.setCellValueFactory(new PropertyValueFactory<>("path"));
        startupPathCol.setPrefWidth(500);

        startupTable.getColumns().addAll(startupNameCol, startupPathCol);
        startupTab.setContent(startupTable);
        
        tabPane.getTabs().addAll(processTab, resourceTab, fileSystemTab, startupTab);

        Scene scene = new Scene(tabPane, 800, 700); // Increased height for charts
        primaryStage.setScene(scene);
        primaryStage.show();
        
        refreshStartupData();
        startAutoRefresh();
    }

    private void killProcess(String pid) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            Process process;
            
            if (osName.contains("win")) {
                // Windows: Use taskkill command
                process = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
            } else {
                // Linux/Unix: Use kill command
                process = Runtime.getRuntime().exec("kill -9 " + pid);
            }
            
            int exitCode = process.waitFor();
            
            Platform.runLater(() -> {
                Alert alert;
                if (exitCode == 0) {
                    alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Process Terminated");
                    alert.setHeaderText("Success");
                    alert.setContentText("Process with PID " + pid + " has been terminated successfully.");
                } else {
                    alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Process Termination Failed");
                    alert.setHeaderText("Error");
                    alert.setContentText("Failed to terminate process with PID " + pid + ". You may not have sufficient permissions.");
                }
                alert.showAndWait();
                
                // Refresh process data after killing
                refreshProcessData();
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Process Termination Error");
                alert.setHeaderText("Exception occurred");
                alert.setContentText("Error terminating process: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}