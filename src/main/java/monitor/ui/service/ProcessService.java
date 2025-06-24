package monitor.ui.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import monitor.ui.model.ProcessInfo;
import monitor.ui.model.Terminatable;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

/**
 * Service class for process-related operations
 * Demonstrates Service Layer pattern and Single Responsibility Principle
 */
public class ProcessService {
    private static final int MAX_PROCESSES_DISPLAYED = 20;
    
    private final SystemInfoFactory factory;
    private final Map<Integer, OSProcess> previousProcessMap;
    private long previousTimestamp;
    
    public ProcessService() {
        this.factory = SystemInfoFactory.getInstance();
        this.previousProcessMap = new HashMap<>();
        this.previousTimestamp = 0;
    }
    
    /**
     * Gets list of top processes by CPU usage
     */
    public List<ProcessInfo> getTopProcesses() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        CentralProcessor processor = si.getHardware().getProcessor();
        int logicalProcessorCount = processor.getLogicalProcessorCount();

        List<OSProcess> processes = os.getProcesses(null, 
            OperatingSystem.ProcessSorting.CPU_DESC, MAX_PROCESSES_DISPLAYED);
        long currentTimestamp = System.currentTimeMillis();
        
        List<ProcessInfo> result = new ArrayList<>();

        for (OSProcess p : processes) {
            double cpu = calculateCpuUsage(p, currentTimestamp, logicalProcessorCount);
            ProcessInfo processInfo = factory.createProcessInfo(p, cpu);
            result.add(processInfo);
        }
        
        // Update previous process map for next calculation
        updatePreviousProcessMap(processes, currentTimestamp);
        
        return result;
    }
    
    /**
     * Terminates a process using the Terminatable interface
     */
    public boolean terminateProcess(Terminatable terminatable) {
        if (terminatable == null || !terminatable.canTerminate()) {
            return false;
        }
        
        try {
            String command = terminatable.getTerminationCommand();
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Error terminating process: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets termination confirmation message
     */
    public String getTerminationConfirmMessage(Terminatable terminatable) {
        if (terminatable == null) {
            return "Invalid process selected.";
        }
        return terminatable.getTerminationConfirmMessage();
    }
    
    // Private helper methods
    private double calculateCpuUsage(OSProcess process, long currentTimestamp, int logicalProcessorCount) {
        double cpu = 0.0;
        if (previousProcessMap.containsKey(process.getProcessID()) && previousTimestamp > 0) {
            OSProcess old = previousProcessMap.get(process.getProcessID());
            long elapsed = currentTimestamp - previousTimestamp;
            if (elapsed > 0) {
                long cputime = process.getKernelTime() + process.getUserTime();
                long oldcputime = old.getKernelTime() + old.getUserTime();
                cpu = ((cputime - oldcputime) * 100.0 / elapsed) / logicalProcessorCount;
            }
        }
        return cpu;
    }
    
    private void updatePreviousProcessMap(List<OSProcess> processes, long currentTimestamp) {
        previousProcessMap.clear();
        for (OSProcess p : processes) {
            previousProcessMap.put(p.getProcessID(), p);
        }
        previousTimestamp = currentTimestamp;
    }
}
