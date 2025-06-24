package monitor.ui.service;

import java.util.ArrayList;
import java.util.List;

import monitor.ui.model.ResourceInfo;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Service class for resource monitoring operations
 * Demonstrates Service Layer pattern and caching
 */
public class ResourceMonitoringService {
    
    private final SystemInfoFactory factory;
    private CpuLoadCache cpuLoadCache;
    
    public ResourceMonitoringService() {
        this.factory = SystemInfoFactory.getInstance();
    }
    
    /**
     * Gets current system resource information
     */
    public List<ResourceInfo> getSystemResources() {
        List<ResourceInfo> resources = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hardware = si.getHardware();
        
        // CPU Resources
        if (cpuLoadCache == null) {
            cpuLoadCache = new CpuLoadCache(hardware.getProcessor());
        }
        
        double[] cpuLoads = cpuLoadCache.getCpuLoads();
        for (int i = 0; i < cpuLoads.length; i++) {
            double coreLoad = cpuLoads[i] * 100;
            ResourceInfo cpuCore = factory.createCpuCoreInfo(i, coreLoad);
            resources.add(cpuCore);
        }
        
        // Memory Resources
        GlobalMemory memory = hardware.getMemory();
        ResourceInfo memoryInfo = createMemoryResourceInfo(memory);
        resources.add(memoryInfo);
        
        // Swap Resources
        ResourceInfo swapInfo = createSwapResourceInfo(memory);
        resources.add(swapInfo);
        
        return resources;
    }
    
    /**
     * Gets CPU core data separately for detailed monitoring
     */
    public List<ResourceInfo> getCpuCoreData() {
        List<ResourceInfo> cpuCores = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        CentralProcessor processor = si.getHardware().getProcessor();
        
        if (cpuLoadCache == null) {
            cpuLoadCache = new CpuLoadCache(processor);
        }
        
        double[] cpuLoads = cpuLoadCache.getCpuLoads();
        for (int i = 0; i < cpuLoads.length; i++) {
            double coreLoad = cpuLoads[i] * 100;
            ResourceInfo cpuCore = factory.createCpuCoreInfo(i, coreLoad);
            cpuCores.add(cpuCore);
        }
        
        return cpuCores;
    }
    
    // Private helper methods
    private ResourceInfo createMemoryResourceInfo(GlobalMemory memory) {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100.0;
        
        String used = String.format("%.2f GB", usedMemory / 1e9);
        String total = String.format("%.2f GB", totalMemory / 1e9);
        
        return factory.createResourceInfo("Memory", memoryUsagePercent, used, total);
    }
    
    private ResourceInfo createSwapResourceInfo(GlobalMemory memory) {
        long totalSwap = memory.getVirtualMemory().getSwapTotal();
        long usedSwap = memory.getVirtualMemory().getSwapUsed();
        double swapUsagePercent = totalSwap > 0 ? (double) usedSwap / totalSwap * 100.0 : 0.0;
        
        String used = String.format("%.2f GB", usedSwap / 1e9);
        String total = String.format("%.2f GB", totalSwap / 1e9);
        
        return factory.createResourceInfo("Swap", swapUsagePercent, used, total);
    }
    
    /**
     * Inner class for CPU load caching - demonstrates encapsulation
     */
    private static class CpuLoadCache {
        private long[][] prevProcTicks;
        private long lastUpdate;
        private double[] lastCpuLoads;
        private final CentralProcessor processor;
        
        CpuLoadCache(CentralProcessor processor) {
            this.processor = processor;
            this.prevProcTicks = processor.getProcessorCpuLoadTicks();
            this.lastUpdate = System.currentTimeMillis();
            this.lastCpuLoads = new double[processor.getLogicalProcessorCount()];
        }
        
        double[] getCpuLoads() {
            long now = System.currentTimeMillis();
            if (now - lastUpdate > 2000) { // Only update every 2s
                lastCpuLoads = processor.getProcessorCpuLoadBetweenTicks(prevProcTicks);
                prevProcTicks = processor.getProcessorCpuLoadTicks();
                lastUpdate = now;
            }
            return lastCpuLoads.clone(); // Return defensive copy
        }
    }
}
