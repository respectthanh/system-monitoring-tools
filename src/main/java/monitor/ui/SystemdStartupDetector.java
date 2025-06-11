package monitor.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import monitor.ui.SystemInfoTable.StartupInfo;

public class SystemdStartupDetector {
    private static final Set<String> DEFAULT_SYSTEMD_UNITS = new HashSet<>(Arrays.asList(
        "basic.target", "multi-user.target", "graphical.target", "rescue.target",
        "emergency.target", "default.target", "shutdown.target", "reboot.target",
        "poweroff.target", "halt.target", "sleep.target", "suspend.target",
        "hibernate.target", "hybrid-sleep.target", "getty.target", "timers.target",
        "sockets.target", "paths.target", "sysinit.target", "local-fs.target",
        "remote-fs.target", "network-online.target", "network-pre.target",
        "network.target", "time-sync.target", "cryptsetup.target", "swap.target",
        "system-update.target",
        
        "systemd-journald.service", "systemd-logind.service", "systemd-udevd.service",
        "systemd-networkd.service", "systemd-resolved.service", "systemd-timesyncd.service",
        "systemd-tmpfiles-clean.service", "systemd-update-utmp.service",
        "systemd-user-sessions.service", "systemd-remount-fs.service",
        "systemd-modules-load.service", "systemd-random-seed.service",
        "systemd-sysctl.service", "getty@.service", "serial-getty@.service",
        "console-getty.service", "dbus.service", "udisks2.service",
        "polkit.service", "gdm.service", "sddm.service", "cron.service",
        "crond.service", "sshd.service", "NetworkManager.service",
        "bluetooth.service", "cups.service", "avahi-daemon.service",
        
        "systemd-journald.socket", "systemd-logind.socket", "dbus.socket",
        "systemd-udevd-control.socket", "systemd-udevd-kernel.socket",
        "cups.socket", "avahi-daemon.socket",
        
        "systemd-tmpfiles-clean.timer", "systemd-reboot.timer",
        
        "systemd-ask-password-console.path", "systemd-ask-password-wall.path",
        
        "-.mount", "dev-hugepages.mount", "dev-mqueue.mount",
        "proc-sys-fs-binfmt_misc.mount", "sys-fs-fuse-connections.mount",
        "sys-kernel-config.mount", "sys-kernel-debug.mount", "sys-kernel-tracing.mount",
        
        "-.slice", "system.slice", "user.slice", "machine.slice"
    ));

    public List<StartupInfo> getSystemdStartupServices() {
        List<StartupInfo> startupServices = new ArrayList<>();
        
        List<String> enabledServices = getEnabledSystemdServices();
        for (String service : enabledServices) {
            if (!isDefaultService(service)) {
                String path = getServiceFilePath(service);
                startupServices.add(new StartupInfo(service, path));
            }
        }
        
        return startupServices;
    }
    
    private boolean isDefaultService(String serviceName) {
        if (DEFAULT_SYSTEMD_UNITS.contains(serviceName)) {
            return true;
        }
        
        for (String defaultUnit : DEFAULT_SYSTEMD_UNITS) {
            if (defaultUnit.endsWith("@.service") && 
                    serviceName.contains("@") && 
                    serviceName.startsWith(defaultUnit.replace("@.service", "@"))) {
                return true;
            }
        }
        
        return false;
    }
    
    private List<String> getEnabledSystemdServices() {
        List<String> enabledServices = new ArrayList<>();
        
        try {
            Process process = Runtime.getRuntime().exec("systemctl list-unit-files --state=enabled --type=service --no-legend");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 1) {
                    enabledServices.add(parts[0]);
                }
            }
            
            process.waitFor();
            reader.close();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting systemd services: " + e.getMessage());
        }
        
        return enabledServices;
    }
    
    private String getServiceFilePath(String serviceName) {
        String[] searchPaths = {
            "/etc/systemd/system/",
            "/usr/lib/systemd/system/",
            "/lib/systemd/system/",
            System.getProperty("user.home") + "/.config/systemd/user/"
        };
        
        for (String path : searchPaths) {
            String fullPath = path + serviceName;
            if (Files.exists(Paths.get(fullPath))) {
                return fullPath;
            }
        }
        
        return "Service file path unknown";
    }
    
    public List<StartupInfo> getCronJobsAtReboot() {
        List<StartupInfo> rebootCronJobs = new ArrayList<>();
        
        // Check user crontab
        try {
            Process process = Runtime.getRuntime().exec("crontab -l");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int jobCounter = 1;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("@reboot")) {
                    String command = line.substring("@reboot".length()).trim();
                    rebootCronJobs.add(new StartupInfo("Cron @reboot #" + jobCounter, command));
                    jobCounter++;
                }
            }
            
            process.waitFor();
            reader.close();
        } catch (IOException | InterruptedException e) {

        }
        
        try {
            File systemCrontab = new File("/etc/crontab");
            if (systemCrontab.exists()) {
                List<String> lines = Files.readAllLines(systemCrontab.toPath());
                int jobCounter = 1;
                
                for (String line : lines) {
                    if (line.trim().startsWith("@reboot")) {
                        String command = line.substring("@reboot".length()).trim();
                        rebootCronJobs.add(new StartupInfo("System Cron @reboot #" + jobCounter, command));
                        jobCounter++;
                    }
                }
            }
        } catch (IOException e) {

        }
        
        return rebootCronJobs;
    }
    
    public List<StartupInfo> getRcLocalEntries() {
        List<StartupInfo> rcLocalEntries = new ArrayList<>();
        
        try {
            File rcLocalFile = new File("/etc/rc.local");
            if (rcLocalFile.exists()) {
                List<String> lines = Files.readAllLines(rcLocalFile.toPath())
                    .stream()
                    .filter(line -> !line.trim().startsWith("#") && !line.trim().isEmpty())
                    .collect(Collectors.toList());
                
                for (int i = 0; i < lines.size(); i++) {
                    String command = lines.get(i).trim();
                    if (!command.equals("exit 0")) {
                        rcLocalEntries.add(new StartupInfo("rc.local entry #" + (i+1), command));
                    }
                }
            }
        } catch (IOException e) {

        }
        
        return rcLocalEntries;
    }
}
