package monitor.ui.service;

import monitor.ui.model.FileSystemInfo;
import monitor.ui.model.GPUInfo;
import monitor.ui.model.ProcessInfo;
import monitor.ui.model.ResourceInfo;
import monitor.ui.model.StartupGroup;
import monitor.ui.model.StartupInfo;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;

/**
 * Factory class for creating system information objects
 * Demonstrates Factory Pattern and encapsulation of object creation logic
 */
public class SystemInfoFactory {
    
    private static SystemInfoFactory instance;
    
    // Singleton pattern
    private SystemInfoFactory() {}
    
    public static SystemInfoFactory getInstance() {
        if (instance == null) {
            synchronized (SystemInfoFactory.class) {
                if (instance == null) {
                    instance = new SystemInfoFactory();
                }
            }
        }
        return instance;
    }
    
    /**
     * Creates ProcessInfo from OSHI OSProcess
     */
    public ProcessInfo createProcessInfo(OSProcess osProcess, double cpuUsage) {
        if (osProcess == null) {
            throw new IllegalArgumentException("OSProcess cannot be null");
        }
        
        double rssMB = osProcess.getResidentSetSize() / (1024.0 * 1024);
        double virtualMemMB = osProcess.getVirtualSize() / (1024.0 * 1024);
        double diskReadMB = osProcess.getBytesRead() / (1024.0 * 1024);
        
        return new ProcessInfo(
            osProcess.getName(),
            osProcess.getUser(),
            String.valueOf(osProcess.getProcessID()),
            Math.max(0.0, cpuUsage),
            rssMB,
            virtualMemMB,
            diskReadMB
        );
    }
    
    /**
     * Creates ResourceInfo for system resources
     */
    public ResourceInfo createResourceInfo(String name, double usedPercent, 
                                         String used, String total) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name cannot be null or empty");
        }
        
        String status = determineResourceStatus(usedPercent);
        return new ResourceInfo(name, status, used, total, usedPercent);
    }
    
    /**
     * Creates ResourceInfo for CPU cores
     */
    public ResourceInfo createCpuCoreInfo(int coreIndex, double coreLoad) {
        String name = String.format("CPU Core %d", coreIndex);
        String status = determineCpuStatus(coreLoad);
        String loadPercent = String.format("%.2f%%", coreLoad);
        
        return new ResourceInfo(name, status, loadPercent, "100%", coreLoad);
    }
    
    /**
     * Creates FileSystemInfo from OSHI OSFileStore
     */
    public FileSystemInfo createFileSystemInfo(OSFileStore fileStore) {
        if (fileStore == null) {
            throw new IllegalArgumentException("OSFileStore cannot be null");
        }
        
        long totalSpace = fileStore.getTotalSpace();
        long usableSpace = fileStore.getUsableSpace();
        long usedSpace = totalSpace - usableSpace;
        
        return new FileSystemInfo(
            fileStore.getMount(),
            fileStore.getName(),
            fileStore.getType(),
            totalSpace,
            usedSpace,
            usableSpace
        );
    }
    
    /**
     * Creates StartupInfo for startup applications
     */
    public StartupInfo createStartupInfo(String name, String path) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Startup application name cannot be null or empty");
        }
        if (path == null) {
            path = "Unknown path";
        }
        
        return new StartupInfo(name, path);
    }
    
    /**
     * Creates StartupGroup for grouping startup applications
     */
    public StartupGroup createStartupGroup(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory path cannot be null or empty");
        }
        
        return new StartupGroup(directoryPath);
    }
    
    /**
     * Creates StartupGroup for individual startup item
     */
    public StartupGroup createStartupGroup(StartupInfo startupInfo) {
        if (startupInfo == null) {
            throw new IllegalArgumentException("StartupInfo cannot be null");
        }
        
        return new StartupGroup(startupInfo);
    }
    
    /**
     * Creates GPUInfo with comprehensive GPU data
     */
    public GPUInfo createGPUInfo(String name, String vendor, long totalMemory, long usedMemory,
                                double gpuUtilization, double temperature, int fanSpeed,
                                long clockSpeed, long memoryClockSpeed, String driverVersion, boolean isDiscrete) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("GPU name cannot be null or empty");
        }
        
        return new GPUInfo(name, vendor, totalMemory, usedMemory, gpuUtilization, 
                          temperature, fanSpeed, clockSpeed, memoryClockSpeed, 
                          driverVersion, isDiscrete);
    }
    
    /**
     * Creates basic GPUInfo with name and vendor only
     */
    public GPUInfo createBasicGPUInfo(String name, String vendor) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("GPU name cannot be null or empty");
        }
        
        return new GPUInfo(name, vendor);
    }
    
    // Helper methods for determining status
    private String determineResourceStatus(double usedPercent) {
        if (usedPercent > 90) return "Critical";
        if (usedPercent > 70) return "High";
        if (usedPercent > 40) return "Medium";
        return "Low";
    }
    
    private String determineCpuStatus(double coreLoad) {
        if (coreLoad > 80) return "High";
        if (coreLoad > 50) return "Medium";
        return "Low";
    }
}
