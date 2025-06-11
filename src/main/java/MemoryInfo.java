
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class MemoryInfo {
    public static void main(String[] args) {
        GlobalMemory memory = new SystemInfo().getHardware().getMemory();

        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        System.out.printf("Tổng RAM: %.2f GB\n", total / 1e9);
        System.out.printf("RAM đang sử dụng: %.2f GB\n", used / 1e9);
    }
}
