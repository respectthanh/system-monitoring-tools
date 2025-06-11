import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class ProcessInfoCollector {
    public static void main(String[] args) throws InterruptedException {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();

        List<OSProcess> initialProcesses = os.getProcesses(null, null, 0);
        Map<Integer, OSProcess> previousMap = new HashMap<>();
        for (OSProcess p : initialProcesses) {
            previousMap.put(p.getProcessID(), p);
        }

        Thread.sleep(20000);

        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 10);

        System.out.println("PID\tName\t\t\tCPU%\tMemory (MB)");
        for (OSProcess process : processes) {
            OSProcess prev = previousMap.get(process.getProcessID());
            double cpu = prev != null ? process.getProcessCpuLoadBetweenTicks(prev) * 100 : 0.0;

            System.out.printf("%d\t%-16s\t%.2f\t%.2f%n",
                    process.getProcessID(),
                    process.getName(),
                    cpu,
                    process.getResidentSetSize() / (1024.0 * 1024)
            );
        }
    }
}
