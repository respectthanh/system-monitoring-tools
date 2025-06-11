import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class CPUInfo {
    public static void main(String[] args) {
        CentralProcessor cpu = new SystemInfo().getHardware().getProcessor();
        System.out.println("Tên CPU: " + cpu.getProcessorIdentifier().getName());
    }
}
