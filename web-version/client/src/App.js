import React, { useState, useEffect } from 'react';
import io from 'socket.io-client';
import {
  Activity,
  HardDrive,
  Settings,
  Users,
  Monitor,
  Cpu,
  MemoryStick,
  Network
} from 'lucide-react';

import ProcessTable from './components/ProcessTable';
import ResourcesTab from './components/ResourcesTab';
import FileSystemTab from './components/FileSystemTab';
import StartupTab from './components/StartupTab';

const tabs = [
  { id: 'processes', name: 'Processes', icon: Users },
  { id: 'resources', name: 'Resources', icon: Activity },
  { id: 'filesystem', name: 'File System', icon: HardDrive },
  { id: 'startup', name: 'Startup', icon: Settings }
];

function App() {
  const [activeTab, setActiveTab] = useState('processes');
  const [systemData, setSystemData] = useState(null);
  const [socket, setSocket] = useState(null);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const newSocket = io(process.env.NODE_ENV === 'production' ? window.location.origin : 'http://localhost:5000');
    
    newSocket.on('connect', () => {
      console.log('Connected to server');
      setConnected(true);
    });

    newSocket.on('disconnect', () => {
      console.log('Disconnected from server');
      setConnected(false);
    });

    newSocket.on('systemData', (data) => {
      setSystemData(data);
    });

    setSocket(newSocket);

    return () => {
      newSocket.close();
    };
  }, []);

  const killProcess = (pid) => {
    if (socket) {
      socket.emit('killProcess', pid);
    }
  };

  const renderTabContent = () => {
    if (!systemData) {
      return (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          <span className="ml-4 text-gray-600">Loading system data...</span>
        </div>
      );
    }

    switch (activeTab) {
      case 'processes':
        return <ProcessTable processes={systemData.processes} onKillProcess={killProcess} />;
      case 'resources':
        return <ResourcesTab data={systemData.resources} history={systemData.history} />;
      case 'filesystem':
        return <FileSystemTab data={systemData.fileSystem} />;
      case 'startup':
        return <StartupTab data={systemData.startup} />;
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-lg border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center">
              <Monitor className="h-8 w-8 text-blue-500" />
              <h1 className="ml-3 text-xl font-bold text-gray-900">System Monitor</h1>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
                connected ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              }`}>
                <div className={`w-2 h-2 rounded-full ${
                  connected ? 'bg-green-500 animate-pulse' : 'bg-red-500'
                }`}></div>
                <span>{connected ? 'Connected' : 'Disconnected'}</span>
              </div>
              
              {systemData && (
                <div className="flex items-center space-x-6 text-sm text-gray-600">
                  <div className="flex items-center space-x-2">
                    <Cpu className="h-4 w-4 text-blue-400" />
                    <span>{systemData.resources?.charts?.cpu?.toFixed(1) || 0}%</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <MemoryStick className="h-4 w-4 text-green-400" />
                    <span>{systemData.resources?.charts?.memory?.toFixed(1) || 0}%</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Network className="h-4 w-4 text-purple-400" />
                    <span>{systemData.resources?.charts?.networkDown?.toFixed(1) || 0} KB/s</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Tab Navigation */}
      <nav className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex space-x-1 py-4">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`flex items-center space-x-2 tab-button ${
                    activeTab === tab.id ? 'tab-active' : 'tab-inactive'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span>{tab.name}</span>
                </button>
              );
            })}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {renderTabContent()}
      </main>
    </div>
  );
}

export default App; 