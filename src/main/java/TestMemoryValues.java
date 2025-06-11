import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class TestMemoryValues {
    public static void main(String[] args) {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hardware = si.getHardware();
        GlobalMemory memory = hardware.getMemory();
        
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        
        System.out.println("=== Memory Information ===");
        System.out.println("Total Memory (bytes): " + totalMemory);
        System.out.println("Available Memory (bytes): " + availableMemory);
        System.out.println("Used Memory (bytes): " + usedMemory);
        System.out.println();
        
        System.out.println("=== Memory in GB (binary) ===");
        System.out.printf("Total Memory: %.2f GB%n", totalMemory / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Available Memory: %.2f GB%n", availableMemory / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Used Memory: %.2f GB%n", usedMemory / (1024.0 * 1024.0 * 1024.0));
        System.out.println();
        
        System.out.println("=== Memory in GB (decimal) ===");
        System.out.printf("Total Memory: %.2f GB%n", totalMemory / 1e9);
        System.out.printf("Available Memory: %.2f GB%n", availableMemory / 1e9);
        System.out.printf("Used Memory: %.2f GB%n", usedMemory / 1e9);
        System.out.println();
        
        long totalSwap = memory.getVirtualMemory().getSwapTotal();
        long usedSwap = memory.getVirtualMemory().getSwapUsed();
        
        System.out.println("=== Swap Information ===");
        System.out.println("Total Swap (bytes): " + totalSwap);
        System.out.println("Used Swap (bytes): " + usedSwap);
        System.out.println();
        
        System.out.println("=== Swap in GB (binary) ===");
        System.out.printf("Total Swap: %.2f GB%n", totalSwap / (1024.0 * 1024.0 * 1024.0));
        System.out.printf("Used Swap: %.2f GB%n", usedSwap / (1024.0 * 1024.0 * 1024.0));
        System.out.println();
        
        System.out.println("=== Swap in GB (decimal) ===");
        System.out.printf("Total Swap: %.2f GB%n", totalSwap / 1e9);
        System.out.printf("Used Swap: %.2f GB%n", usedSwap / 1e9);
    }
}
