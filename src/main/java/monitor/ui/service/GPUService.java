package monitor.ui.service;

import monitor.ui.model.GPUInfo;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for GPU monitoring operations
 * Demonstrates Service Layer pattern with GPU-specific functionality
 */
public class GPUService {
    
    private final SystemInfoFactory factory;
    private long lastGpuFetch = 0;
    private List<GPUInfo> lastGpuCache = new ArrayList<>();
    private static final long CACHE_DURATION_MS = 5000; // 5 seconds cache for GPU data
    
    public GPUService() {
        this.factory = SystemInfoFactory.getInstance();
    }
    
    /**
     * Gets all available GPU information with caching
     */
    public List<GPUInfo> getGPUInfo() {
        long now = System.currentTimeMillis();
        if (now - lastGpuFetch < CACHE_DURATION_MS && !lastGpuCache.isEmpty()) {
            return new ArrayList<>(lastGpuCache); // Return defensive copy
        }
        
        List<GPUInfo> gpus = new ArrayList<>();
        
        try {
            SystemInfo si = new SystemInfo();
            HardwareAbstractionLayer hardware = si.getHardware();
            List<GraphicsCard> graphicsCards = hardware.getGraphicsCards();
            
            if (graphicsCards != null && !graphicsCards.isEmpty()) {
                for (GraphicsCard card : graphicsCards) {
                    GPUInfo gpuInfo = createGPUInfoFromCard(card);
                    gpus.add(gpuInfo);
                }
            } else {
                // If no graphics cards detected, add a fallback entry
                gpus.add(new GPUInfo("No GPU Detected", "Unknown"));
            }
            
        } catch (Exception e) {
            System.err.println("Error reading GPU information: " + e.getMessage());
            // Add error entry so UI doesn't break
            gpus.add(new GPUInfo("Error Reading GPU", "Error: " + e.getMessage()));
        }
        
        lastGpuCache = gpus;
        lastGpuFetch = now;
        return gpus;
    }
    
    /**
     * Gets GPU statistics summary
     */
    public GPUStatistics getGPUStatistics() {
        List<GPUInfo> gpus = getGPUInfo();
        
        int totalGPUs = gpus.size();
        int discreteGPUs = 0;
        int integratedGPUs = 0;
        long totalVRAM = 0;
        long usedVRAM = 0;
        double maxUtilization = 0.0;
        double maxTemperature = 0.0;
        String primaryGPU = "None";
        
        for (GPUInfo gpu : gpus) {
            if (gpu.isDiscrete()) {
                discreteGPUs++;
            } else {
                integratedGPUs++;
            }
            
            totalVRAM += gpu.getTotalMemory();
            usedVRAM += gpu.getUsedMemory();
            
            if (gpu.getGpuUtilization() > maxUtilization) {
                maxUtilization = gpu.getGpuUtilization();
                primaryGPU = gpu.getName();
            }
            
            if (gpu.getTemperature() > maxTemperature) {
                maxTemperature = gpu.getTemperature();
            }
        }
        
        return new GPUStatistics(totalGPUs, discreteGPUs, integratedGPUs, 
                               totalVRAM, usedVRAM, maxUtilization, maxTemperature, primaryGPU);
    }
    
    /**
     * Gets primary (most active) GPU
     */
    public GPUInfo getPrimaryGPU() {
        List<GPUInfo> gpus = getGPUInfo();
        
        if (gpus.isEmpty()) {
            return new GPUInfo("No GPU", "Unknown");
        }
        
        // Return the GPU with highest utilization, or first discrete GPU, or first GPU
        GPUInfo primary = gpus.get(0);
        double maxUtilization = -1;
        
        for (GPUInfo gpu : gpus) {
            if (gpu.getGpuUtilization() > maxUtilization || 
                (primary.isDiscrete() == gpu.isDiscrete() && gpu.isDiscrete())) {
                maxUtilization = gpu.getGpuUtilization();
                primary = gpu;
            }
        }
        
        return primary;
    }
    
    /**
     * Forces cache refresh
     */
    public void refreshCache() {
        lastGpuFetch = 0;
        lastGpuCache.clear();
    }
    
    /**
     * Gets discrete GPUs only
     */
    public List<GPUInfo> getDiscreteGPUs() {
        return getGPUInfo().stream()
                .filter(GPUInfo::isDiscrete)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets integrated GPUs only
     */
    public List<GPUInfo> getIntegratedGPUs() {
        return getGPUInfo().stream()
                .filter(gpu -> !gpu.isDiscrete())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Checks if any GPU is overheating (>85°C)
     */
    public boolean isAnyGPUOverheating() {
        return getGPUInfo().stream()
                .anyMatch(gpu -> gpu.getTemperature() > 85.0);
    }
    
    /**
     * Gets GPUs that are under heavy load (>80% utilization)
     */
    public List<GPUInfo> getHighUtilizationGPUs() {
        return getGPUInfo().stream()
                .filter(gpu -> gpu.getGpuUtilization() > 80.0)
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Private helper methods
    private GPUInfo createGPUInfoFromCard(GraphicsCard card) {
        try {
            String name = card.getName() != null ? card.getName() : "Unknown GPU";
            String vendor = card.getVendor() != null ? card.getVendor() : "Unknown";
            
            // OSHI provides basic GPU info - some advanced metrics may not be available
            long totalMemory = card.getVRam();
            
            // Note: OSHI doesn't provide real-time GPU utilization, temperature, etc.
            // For production use, you might need platform-specific libraries like:
            // - NVIDIA ML (for NVIDIA GPUs)
            // - AMD ADL (for AMD GPUs)
            // - Intel GPU libraries
            
            String driverVersion = card.getVersionInfo() != null ? card.getVersionInfo() : "Unknown";
            
            // Detect if discrete GPU based on vendor and memory
            boolean isDiscrete = totalMemory > 1024 * 1024 * 1024 || // > 1GB VRAM
                               vendor.toLowerCase().contains("nvidia") ||
                               vendor.toLowerCase().contains("amd") ||
                               vendor.toLowerCase().contains("ati");
            
            return factory.createGPUInfo(name, vendor, totalMemory, 0, 0.0, 0.0, 
                                       0, 0, 0, driverVersion, isDiscrete);
                                       
        } catch (Exception e) {
            System.err.println("Error creating GPU info from card: " + e.getMessage());
            return new GPUInfo("Error Reading GPU", "Error");
        }
    }
    
    /**
     * GPU statistics data class
     */
    public static class GPUStatistics {
        private final int totalGPUs;
        private final int discreteGPUs;
        private final int integratedGPUs;
        private final long totalVRAM;
        private final long usedVRAM;
        private final double maxUtilization;
        private final double maxTemperature;
        private final String primaryGPU;
        
        public GPUStatistics(int totalGPUs, int discreteGPUs, int integratedGPUs,
                           long totalVRAM, long usedVRAM, double maxUtilization,
                           double maxTemperature, String primaryGPU) {
            this.totalGPUs = totalGPUs;
            this.discreteGPUs = discreteGPUs;
            this.integratedGPUs = integratedGPUs;
            this.totalVRAM = totalVRAM;
            this.usedVRAM = usedVRAM;
            this.maxUtilization = maxUtilization;
            this.maxTemperature = maxTemperature;
            this.primaryGPU = primaryGPU;
        }
        
        // Getters
        public int getTotalGPUs() { return totalGPUs; }
        public int getDiscreteGPUs() { return discreteGPUs; }
        public int getIntegratedGPUs() { return integratedGPUs; }
        public long getTotalVRAM() { return totalVRAM; }
        public long getUsedVRAM() { return usedVRAM; }
        public long getAvailableVRAM() { return totalVRAM - usedVRAM; }
        public double getMaxUtilization() { return maxUtilization; }
        public double getMaxTemperature() { return maxTemperature; }
        public String getPrimaryGPU() { return primaryGPU; }
        
        public double getVRAMUsagePercentage() {
            return totalVRAM > 0 ? (double) usedVRAM / totalVRAM * 100.0 : 0.0;
        }
        
        public String getFormattedTotalVRAM() {
            return formatBytes(totalVRAM);
        }
        
        public String getFormattedUsedVRAM() {
            return formatBytes(usedVRAM);
        }
        
        public String getFormattedAvailableVRAM() {
            return formatBytes(getAvailableVRAM());
        }
        
        private String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp-1) + "";
            return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
        }
    }
} 