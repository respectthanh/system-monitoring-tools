# System Monitor Web Version

A modern, web-based system monitoring dashboard that provides real-time insights into your system's performance.

## Features

- **Process Monitoring**: Real-time process list with CPU, memory usage, and kill functionality
- **Resource Monitoring**: CPU, Memory, Swap, Network usage with historical charts  
- **File System Monitoring**: Disk usage overview for all mounted file systems
- **Startup Applications**: System services, cron jobs, and rc.local entries
- **Modern Interface**: Dark theme, responsive design, real-time updates

## Quick Start

1. **Install dependencies**:
   ```bash
   npm run install-all
   ```

2. **Start development servers**:
   ```bash
   npm run dev
   ```

3. **Open browser**: http://localhost:3000

## Production Deployment

```bash
npm run build
npm start
```

Application available at http://localhost:5000

## Architecture

- **Backend**: Node.js + Express + Socket.IO
- **Frontend**: React + Tailwind CSS + Chart.js
- **Real-time**: WebSocket communication
- **System Info**: systeminformation + ps-list libraries

## Requirements

- Node.js 16+
- Linux system
- Appropriate permissions for process management