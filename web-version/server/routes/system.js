import express from 'express';
import SystemMonitor from '../services/SystemMonitor.js';

const router = express.Router();

const systemMonitor = new SystemMonitor();

// Get all system information
router.get('/', async (req, res) => {
  try {
    const data = await systemMonitor.getAllSystemInfo();
    res.json(data);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get process information
router.get('/processes', async (req, res) => {
  try {
    const processes = await systemMonitor.getProcessInfo();
    res.json(processes);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get resource information
router.get('/resources', async (req, res) => {
  try {
    const resources = await systemMonitor.getResourceInfo();
    res.json(resources);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get filesystem information
router.get('/filesystem', async (req, res) => {
  try {
    const filesystem = await systemMonitor.getFileSystemInfo();
    res.json(filesystem);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get startup applications
router.get('/startup', async (req, res) => {
  try {
    const startup = await systemMonitor.getStartupInfo();
    res.json(startup);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Kill a process
router.post('/processes/:pid/kill', async (req, res) => {
  try {
    const { pid } = req.params;
    await systemMonitor.killProcess(pid);
    res.json({ success: true, message: `Process ${pid} killed successfully` });
  } catch (error) {
    res.status(500).json({ success: false, error: error.message });
  }
});

export default router; 