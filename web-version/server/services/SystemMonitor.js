import si from 'systeminformation';
import psList from 'ps-list';
import { exec } from 'child_process';
import { promises as fs } from 'fs';
import path from 'path';

class SystemMonitor {
  constructor() {
    this.previousCpuTicks = null;
    this.previousNetworkStats = null;
    this.history = {
      cpu: [],
      memory: [],
      swap: [],
      networkUp: [],
      networkDown: []
    };
    this.maxHistoryPoints = 60; // 60 data points for history
  }

  async getAllSystemInfo() {
    try {
      const [
        processes,
        resources,
        fileSystem,
        startup
      ] = await Promise.all([
        this.getProcessInfo(),
        this.getResourceInfo(),
        this.getFileSystemInfo(),
        this.getStartupInfo()
      ]);

      return {
        processes,
        resources,
        fileSystem,
        startup,
        history: this.history,
        timestamp: Date.now()
      };
    } catch (error) {
      console.error('Error getting system info:', error);
      throw error;
    }
  }

  async getProcessInfo() {
    try {
      // Use systeminformation for better process data
      const processes = await si.processes();
      
      // Get top 100 processes sorted by CPU usage
      const processesWithDetails = processes.list
        .slice(0, 100)
        .map((proc) => ({
          pid: proc.pid.toString(),
          name: proc.name || proc.command || 'Unknown',
          user: proc.user || 'Unknown',
          cpu: (proc.pcpu || 0).toFixed(2),
          cpuValue: proc.pcpu || 0,
          rss: ((proc.mem || 0) / (1024 * 1024)).toFixed(2), // Convert to MB
          rssValue: (proc.mem || 0) / (1024 * 1024),
          virtualMem: ((proc.memVsz || 0) / 1024).toFixed(2), // Convert to MB
          virtualMemValue: (proc.memVsz || 0) / 1024,
          diskRead: "0.00", // Not readily available
          diskReadValue: 0
        }))
        .sort((a, b) => b.cpuValue - a.cpuValue);

      return processesWithDetails;
    } catch (error) {
      console.error('Error getting process info:', error);
      
      // Fallback to ps-list if systeminformation fails
      try {
        const processes = await psList();
        return processes.slice(0, 100).map((proc) => ({
          pid: proc.pid.toString(),
          name: proc.name || 'Unknown',
          user: proc.username || 'Unknown',
          cpu: "0.00",
          cpuValue: 0,
          rss: ((proc.memory || 0) / (1024 * 1024)).toFixed(2),
          rssValue: (proc.memory || 0) / (1024 * 1024),
          virtualMem: "0.00",
          virtualMemValue: 0,
          diskRead: "0.00",
          diskReadValue: 0
        }));
      } catch (fallbackError) {
        console.error('Fallback process info failed:', fallbackError);
        return [];
      }
    }
  }

  async getResourceInfo() {
    try {
      const [
        cpuLoad,
        memory,
        networkStats
      ] = await Promise.all([
        si.currentLoad(),
        si.mem(),
        si.networkStats()
      ]);

      // Calculate network usage
      let networkUp = 0;
      let networkDown = 0;
      
      if (this.previousNetworkStats) {
        const timeDiff = (Date.now() - this.previousNetworkStats.timestamp) / 1000; // in seconds
        if (timeDiff > 0) {
          const totalTx = networkStats.reduce((sum, iface) => sum + (iface.tx_bytes || 0), 0);
          const totalRx = networkStats.reduce((sum, iface) => sum + (iface.rx_bytes || 0), 0);
          
          networkUp = Math.max(0, (totalTx - this.previousNetworkStats.tx) / timeDiff / 1024); // KB/s
          networkDown = Math.max(0, (totalRx - this.previousNetworkStats.rx) / timeDiff / 1024); // KB/s
        }
      }

      this.previousNetworkStats = {
        tx: networkStats.reduce((sum, iface) => sum + (iface.tx_bytes || 0), 0),
        rx: networkStats.reduce((sum, iface) => sum + (iface.rx_bytes || 0), 0),
        timestamp: Date.now()
      };

      const cpuUsage = cpuLoad.currentLoad || 0;
      const memUsage = (memory.used / memory.total) * 100;
      const swapUsage = memory.swaptotal > 0 ? (memory.swapused / memory.swaptotal) * 100 : 0;

      // Update history
      this.updateHistory('cpu', cpuUsage);
      this.updateHistory('memory', memUsage);
      this.updateHistory('swap', swapUsage);
      this.updateHistory('networkUp', networkUp);
      this.updateHistory('networkDown', networkDown);

      // Get CPU core information
      const cpuCores = cpuLoad.cpus.map((core, index) => ({
        name: `Core ${index}`,
        status: `${core.load.toFixed(2)}%`,
        used: `${core.load.toFixed(2)}%`,
        total: "100%",
        usedPercent: core.load
      }));

      const resources = [
        {
          name: 'CPU',
          status: `${cpuUsage.toFixed(2)}%`,
          used: `${cpuUsage.toFixed(2)}%`,
          total: '100%',
          usedPercent: cpuUsage
        },
        {
          name: 'Memory',
          status: `${(memory.used / (1024 * 1024 * 1024)).toFixed(2)} GB`,
          used: `${(memory.used / (1024 * 1024 * 1024)).toFixed(2)} GB`,
          total: `${(memory.total / (1024 * 1024 * 1024)).toFixed(2)} GB`,
          usedPercent: memUsage
        },
        {
          name: 'Swap',
          status: memory.swaptotal > 0 ? `${(memory.swapused / (1024 * 1024 * 1024)).toFixed(2)} GB` : 'N/A',
          used: memory.swaptotal > 0 ? `${(memory.swapused / (1024 * 1024 * 1024)).toFixed(2)} GB` : '0 GB',
          total: memory.swaptotal > 0 ? `${(memory.swaptotal / (1024 * 1024 * 1024)).toFixed(2)} GB` : '0 GB',
          usedPercent: swapUsage
        },
        {
          name: 'Network Up',
          status: `${networkUp.toFixed(2)} KB/s`,
          used: `${networkUp.toFixed(2)} KB/s`,
          total: '∞',
          usedPercent: 0
        },
        {
          name: 'Network Down',
          status: `${networkDown.toFixed(2)} KB/s`,
          used: `${networkDown.toFixed(2)} KB/s`,
          total: '∞',
          usedPercent: 0
        }
      ];

      return {
        resources,
        cpuCores,
        charts: {
          cpu: cpuUsage,
          memory: memUsage,
          swap: swapUsage,
          networkUp,
          networkDown
        }
      };
    } catch (error) {
      console.error('Error getting resource info:', error);
      return { resources: [], cpuCores: [], charts: {} };
    }
  }

  async getFileSystemInfo() {
    try {
      const disks = await si.fsSize();
      
      return disks.map(disk => ({
        mountPoint: disk.mount,
        name: disk.fs,
        type: disk.type,
        totalSpace: `${(disk.size / (1024 * 1024 * 1024)).toFixed(2)} GB`,
        usedSpace: `${(disk.used / (1024 * 1024 * 1024)).toFixed(2)} GB`,
        usableSpace: `${((disk.size - disk.used) / (1024 * 1024 * 1024)).toFixed(2)} GB`,
        usedPercent: disk.size > 0 ? (disk.used / disk.size) * 100 : 0
      }));
    } catch (error) {
      console.error('Error getting filesystem info:', error);
      return [];
    }
  }

  async getStartupInfo() {
    try {
      const [
        systemdServices,
        cronJobs,
        rcLocalEntries
      ] = await Promise.all([
        this.getSystemdServices(),
        this.getCronJobs(),
        this.getRcLocalEntries()
      ]);

      return [
        ...systemdServices,
        ...cronJobs,
        ...rcLocalEntries
      ];
    } catch (error) {
      console.error('Error getting startup info:', error);
      return [];
    }
  }

  async getSystemdServices() {
    return new Promise((resolve) => {
      exec('systemctl list-unit-files --state=enabled --type=service --no-legend', (error, stdout) => {
        if (error) {
          resolve([]);
          return;
        }

        const services = stdout.split('\n')
          .filter(line => line.trim())
          .map(line => {
            const parts = line.trim().split(/\s+/);
            const serviceName = parts[0];
            return {
              name: serviceName,
              path: `/etc/systemd/system/${serviceName}`,
              type: 'systemd'
            };
          })
          .filter(service => !this.isDefaultSystemdService(service.name));

        resolve(services);
      });
    });
  }

  async getCronJobs() {
    return new Promise((resolve) => {
      exec('crontab -l', (error, stdout) => {
        const cronJobs = [];
        
        if (!error && stdout) {
          const lines = stdout.split('\n');
          let jobCounter = 1;
          
          lines.forEach(line => {
            if (line.trim().startsWith('@reboot')) {
              const command = line.substring('@reboot'.length()).trim();
              cronJobs.push({
                name: `Cron @reboot #${jobCounter}`,
                path: command,
                type: 'cron'
              });
              jobCounter++;
            }
          });
        }

        resolve(cronJobs);
      });
    });
  }

  async getRcLocalEntries() {
    try {
      const rcLocalPath = '/etc/rc.local';
      const content = await fs.readFile(rcLocalPath, 'utf8');
      const lines = content.split('\n')
        .filter(line => !line.trim().startsWith('#') && line.trim() && line.trim() !== 'exit 0');

      return lines.map((line, index) => ({
        name: `rc.local entry #${index + 1}`,
        path: line.trim(),
        type: 'rc.local'
      }));
    } catch (error) {
      return [];
    }
  }

  isDefaultSystemdService(serviceName) {
    const defaultServices = [
      'systemd-journald.service', 'systemd-logind.service', 'systemd-udevd.service',
      'systemd-networkd.service', 'systemd-resolved.service', 'systemd-timesyncd.service',
      'dbus.service', 'NetworkManager.service', 'gdm.service', 'sddm.service',
      'ssh.service', 'sshd.service', 'bluetooth.service', 'cups.service'
    ];

    return defaultServices.includes(serviceName) || 
           serviceName.startsWith('getty@') || 
           serviceName.startsWith('systemd-');
  }

  updateHistory(metric, value) {
    if (!this.history[metric]) {
      this.history[metric] = [];
    }
    
    this.history[metric].push({
      timestamp: Date.now(),
      value: value
    });

    // Keep only the last maxHistoryPoints
    if (this.history[metric].length > this.maxHistoryPoints) {
      this.history[metric] = this.history[metric].slice(-this.maxHistoryPoints);
    }
  }

  async killProcess(pid) {
    return new Promise((resolve, reject) => {
      exec(`kill ${pid}`, (error) => {
        if (error) {
          reject(new Error(`Failed to kill process ${pid}: ${error.message}`));
        } else {
          resolve();
        }
      });
    });
  }
}

export default SystemMonitor; 