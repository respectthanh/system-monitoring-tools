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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
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
import oshi.hardware.NetworkIF;
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
    
    private XYChart.Series<String, Number> cpuChartSeries;
    private PieChart memoryChart;
    private PieChart swapChart;
    private TableView<ResourceInfo> cpuTableView;

    private final Map<Integer, OSProcess> previousProcessMap = new HashMap<>();
    private long previousTimestamp;
    private TableView<ProcessInfo> processTable; // Thêm biến instance

    private static final int MAX_DATA_POINTS = 60; // For 60 seconds of history
    private final XYChart.Series<String, Number> cpuHistory = new XYChart.Series<>();
    private final XYChart.Series<String, Number> memHistory = new XYChart.Series<>();
    private final XYChart.Series<String, Number> swapHistory = new XYChart.Series<>();
    private final XYChart.Series<String, Number> netUpHistory = new XYChart.Series<>();
    private final XYChart.Series<String, Number> netDownHistory = new XYChart.Series<>();

    private long[] prevTotalTicks;
    private long[][] prevProcTicks;
    private long networkTimestamp;
    private long bytesSent;
    private long bytesRecv;

    public static class ResourceSnapshot {
        final double cpuLoad;
        final double memLoad;
        final double swapLoad;
        final double netUp;
        final double netDown;
        final List<ResourceInfo> cpuCores;

        public ResourceSnapshot(double cpuLoad, double memLoad, double swapLoad, double netUp, double netDown, List<ResourceInfo> cpuCores) {
            this.cpuLoad = cpuLoad;
            this.memLoad = memLoad;
            this.swapLoad = swapLoad;
            this.netUp = netUp;
            this.netDown = netDown;
            this.cpuCores = cpuCores;
        }
    }

    // Inner class definitions (ensured they are present)
    public static class ProcessInfo {
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessInfo that = (ProcessInfo) o;
            return pid.equals(that.pid);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(pid);
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
                if (old != null) {
                    long elapsed = currentTimestamp - previousTimestamp;
                    if (elapsed > 0) {
                        long cputime = p.getKernelTime() + p.getUserTime();
                        long oldcputime = old.getKernelTime() + old.getUserTime();
                        cpu = ((cputime - oldcputime) * 100.0 / elapsed) / logicalProcessorCount;
                    }
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

    private void killProcess(String pid) {
        String osName = System.getProperty("os.name").toLowerCase();
        String command;
        if (osName.contains("win")) {
            command = "taskkill /F /PID " + pid;
        } else {
            command = "kill -9 " + pid;
        }

        Task<Void> killTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Process process = Runtime.getRuntime().exec(command);
                process.waitFor();
                if (process.exitValue() != 0) {
                    throw new RuntimeException("Process exited with non-zero value: " + process.exitValue());
                }
                return null;
            }
        };

        killTask.setOnSucceeded(e -> Platform.runLater(this::refreshProcessData));

        killTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to End Process");
                alert.setContentText("Could not terminate process with PID: " + pid + ". It might have already been terminated or you may not have the necessary permissions.");
                alert.showAndWait();
            });
        });

        new Thread(killTask).start();
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

    private ResourceSnapshot getSystemResources() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hardware = si.getHardware();
        CentralProcessor processor = hardware.getProcessor();

        // CPU calculation
        long[] currentTotalTicks = processor.getSystemCpuLoadTicks();
        double cpuLoad = 0.0;
        if (prevTotalTicks != null) {
            cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTotalTicks) * 100;
        }
        prevTotalTicks = currentTotalTicks;

        // Per-core CPU
        List<ResourceInfo> coreData = new ArrayList<>();
        long[][] currentProcTicks = processor.getProcessorCpuLoadTicks();
        if (prevProcTicks != null) {
            double[] coreLoads = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
            for (int i = 0; i < coreLoads.length; i++) {
                double coreLoad = coreLoads[i] * 100;
                coreData.add(new ResourceInfo(
                    String.format("CPU Core %d", i),
                    String.format("%.2f%%", coreLoad),
                    coreLoad > 50 ? "High" : coreLoad > 20 ? "Medium" : "Low",
                    "100%",
                    coreLoad
                ));
            }
        }
        prevProcTicks = currentProcTicks;

        // Memory usage
        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        double memLoad = totalMemory > 0 ? (double)(totalMemory - availableMemory) / totalMemory * 100.0 : 0.0;

        // Swap usage
        long totalSwap = memory.getVirtualMemory().getSwapTotal();
        long usedSwap = memory.getVirtualMemory().getSwapUsed();
        double swapLoad = totalSwap > 0 ? (double)usedSwap / totalSwap * 100.0 : 0.0;

        // Network usage
        long currentBytesSent = 0;
        long currentBytesRecv = 0;
        for (NetworkIF net : hardware.getNetworkIFs()) {
            currentBytesSent += net.getBytesSent();
            currentBytesRecv += net.getBytesRecv();
        }
        long currentNetworkTimestamp = System.currentTimeMillis();
        double netUp = 0.0;
        double netDown = 0.0;
        if (networkTimestamp > 0) {
            long timeDiff = currentNetworkTimestamp - networkTimestamp;
            if (timeDiff > 0) {
                netUp = (currentBytesSent - this.bytesSent) / (timeDiff / 1000.0) / 1024.0; // KB/s
                netDown = (currentBytesRecv - this.bytesRecv) / (timeDiff / 1000.0) / 1024.0; // KB/s
            }
        }
        this.networkTimestamp = currentNetworkTimestamp;
        this.bytesSent = currentBytesSent;
        this.bytesRecv = currentBytesRecv;

        return new ResourceSnapshot(Math.max(0.0, cpuLoad), memLoad, swapLoad, Math.max(0.0, netUp), Math.max(0.0, netDown), coreData);
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
                ProcessInfo selectedItem = processTable.getSelectionModel().getSelectedItem();
                List<TableColumn<ProcessInfo, ?>> sortOrder = new ArrayList<>(processTable.getSortOrder());

                processData.clear();
                processData.addAll(newData);

                if (!sortOrder.isEmpty()) {
                    processTable.getSortOrder().addAll(sortOrder);
                    processTable.sort();
                }

                if (selectedItem != null) {
                    processTable.getSelectionModel().select(selectedItem);
                    processTable.scrollTo(selectedItem);
                }
            });
        });
        
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh process data: " + task.getException().getMessage());
        });
        
        new Thread(task).start();
    }
    
    private void refreshResourceData() {
        Task<ResourceSnapshot> task = new Task<ResourceSnapshot>() {
            @Override
            protected ResourceSnapshot call() throws Exception {
                return getSystemResources();
            }
        };
        
        task.setOnSucceeded(e -> {
            ResourceSnapshot snapshot = task.getValue();
            Platform.runLater(() -> {
                updateHistoryCharts(snapshot);
            });
        });
        
        task.setOnFailed(e -> {
            System.err.println("Failed to refresh resource data: " + task.getException().getMessage());
            task.getException().printStackTrace();
        });
        
        new Thread(task).start();
    }
    
    private void updateHistoryCharts(ResourceSnapshot snapshot) {
        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm:ss");
        String time = format.format(new java.util.Date(now));

        // CPU
        cpuHistory.getData().add(new XYChart.Data<>(time, snapshot.cpuLoad));
        if (cpuHistory.getData().size() > MAX_DATA_POINTS) cpuHistory.getData().remove(0);

        // Memory
        memHistory.getData().add(new XYChart.Data<>(time, snapshot.memLoad));
        if (memHistory.getData().size() > MAX_DATA_POINTS) memHistory.getData().remove(0);

        // Swap
        swapHistory.getData().add(new XYChart.Data<>(time, snapshot.swapLoad));
        if (swapHistory.getData().size() > MAX_DATA_POINTS) swapHistory.getData().remove(0);
        
        // Network
        netUpHistory.getData().add(new XYChart.Data<>(time, snapshot.netUp));
        if (netUpHistory.getData().size() > MAX_DATA_POINTS) netUpHistory.getData().remove(0);
        
        netDownHistory.getData().add(new XYChart.Data<>(time, snapshot.netDown));
        if (netDownHistory.getData().size() > MAX_DATA_POINTS) netDownHistory.getData().remove(0);

        // Update CPU core table
        cpuCoreData.clear();
        cpuCoreData.addAll(snapshot.cpuCores);
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

    private LineChart<String, Number> createHistoryChart(String title, String yAxisLabel, XYChart.Series<String, Number>... series) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Time");
        xAxis.setTickLabelsVisible(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(yAxisLabel);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.getData().addAll(series);
        chart.setPrefHeight(200);

        return chart;
    }

    private VBox createResourceCharts() {
        cpuHistory.setName("CPU");
        memHistory.setName("Memory");
        swapHistory.setName("Swap");

        LineChart<String, Number> cpuChart = createHistoryChart("CPU Usage (%)", "Usage %", cpuHistory);
        LineChart<String, Number> memChart = createHistoryChart("Memory Usage (%)", "Usage %", memHistory);
        LineChart<String, Number> swapChart = createHistoryChart("Swap Usage (%)", "Usage %", swapHistory);
        
        netUpHistory.setName("Upload");
        netDownHistory.setName("Download");
        LineChart<String, Number> netChart = createHistoryChart("Network (KB/s)", "KB/s", netUpHistory, netDownHistory);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.add(cpuChart, 0, 0);
        gridPane.add(memChart, 1, 0);
        gridPane.add(swapChart, 0, 1);
        gridPane.add(netChart, 1, 1);

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

        VBox container = new VBox(10, gridPane, cpuTableView);
        container.setPadding(new Insets(10));
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

        Button endProcessButton = new Button("End Process");
        endProcessButton.disableProperty().bind(processTable.getSelectionModel().selectedItemProperty().isNull());
        endProcessButton.setOnAction(event -> {
            ProcessInfo selectedProcess = processTable.getSelectionModel().getSelectedItem();
            if (selectedProcess != null) {
                killProcess(selectedProcess.getPid());
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

    public static void main(String[] args) {
        launch(args);
    }
}