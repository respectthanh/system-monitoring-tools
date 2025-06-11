import express from 'express';
import { createServer } from 'http';
import { Server } from 'socket.io';
import cors from 'cors';
import path from 'path';
import { fileURLToPath } from 'url';
import cron from 'node-cron';

import systemRoutes from './routes/system.js';
import SystemMonitor from './services/SystemMonitor.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const server = createServer(app);
const io = new Server(server, {
  cors: {
    origin: process.env.NODE_ENV === 'production' ? false : "http://localhost:3000",
    methods: ["GET", "POST"]
  }
});

const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, '../client/build')));

// Initialize system monitor
const systemMonitor = new SystemMonitor();

// Routes
app.use('/api/system', systemRoutes);

// Serve React app
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, '../client/build/index.html'));
});

// Socket.IO connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);

  // Send initial data
  systemMonitor.getAllSystemInfo().then(data => {
    console.log(`Sending data: ${data.processes.length} processes, ${data.fileSystem.length} filesystems`);
    socket.emit('systemData', data);
  }).catch(error => {
    console.error('Error getting initial system data:', error);
  });

  // Handle disconnection
  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
  });

  // Handle process kill request
  socket.on('killProcess', async (pid) => {
    try {
      await systemMonitor.killProcess(pid);
      socket.emit('processKilled', { success: true, pid });
    } catch (error) {
      socket.emit('processKilled', { success: false, pid, error: error.message });
    }
  });
});

// Real-time data updates every 2 seconds
cron.schedule('*/2 * * * * *', async () => {
  try {
    const systemData = await systemMonitor.getAllSystemInfo();
    io.emit('systemData', systemData);
  } catch (error) {
    console.error('Error getting system data:', error);
  }
});

server.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
}); 