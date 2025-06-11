import React from 'react';
import { HardDrive, Database, Folder } from 'lucide-react';

const FileSystemTab = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div className="space-y-6">
        <h2 className="text-2xl font-bold text-gray-900">File System</h2>
        <div className="card text-center py-12">
          <HardDrive className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500">No filesystem data available</p>
        </div>
      </div>
    );
  }

  const getUsageColor = (percentage) => {
    if (percentage > 90) return 'bg-red-500';
    if (percentage > 75) return 'bg-yellow-500';
    if (percentage > 50) return 'bg-blue-500';
    return 'bg-green-500';
  };

  const getUsageTextColor = (percentage) => {
    if (percentage > 90) return 'text-red-400';
    if (percentage > 75) return 'text-yellow-400';
    if (percentage > 50) return 'text-blue-400';
    return 'text-green-400';
  };

  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getFileSystemIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'ext4':
      case 'ext3':
      case 'ext2':
        return <Database className="h-5 w-5 text-blue-400" />;
      case 'ntfs':
      case 'fat32':
      case 'fat':
        return <HardDrive className="h-5 w-5 text-purple-400" />;
      case 'tmpfs':
      case 'proc':
      case 'sysfs':
        return <Folder className="h-5 w-5 text-yellow-400" />;
      default:
        return <HardDrive className="h-5 w-5 text-gray-400" />;
    }
  };

  // Calculate total storage statistics
  const totalStats = data.reduce((acc, disk) => {
    if (disk.type !== 'tmpfs' && disk.type !== 'proc' && disk.type !== 'sysfs') {
      const totalBytes = parseFloat(disk.totalSpace.replace(' GB', '')) * 1024 * 1024 * 1024;
      const usedBytes = parseFloat(disk.usedSpace.replace(' GB', '')) * 1024 * 1024 * 1024;
      acc.total += totalBytes;
      acc.used += usedBytes;
    }
    return acc;
  }, { total: 0, used: 0 });

  const totalUsagePercent = totalStats.total > 0 ? (totalStats.used / totalStats.total) * 100 : 0;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">File System</h2>
        <div className="text-sm text-gray-600">
          {data.length} mount points
        </div>
      </div>

      {/* Overall Storage Summary */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <HardDrive className="h-5 w-5 text-blue-400 mr-2" />
          Overall Storage Usage
        </h3>
        <div className="flex items-center space-x-4">
          <div className="flex-1">
            <div className="flex justify-between text-sm text-gray-700 mb-2">
              <span>Used: {formatBytes(totalStats.used)}</span>
              <span>Total: {formatBytes(totalStats.total)}</span>
            </div>
            <div className="progress-bar">
              <div 
                className={`progress-fill ${getUsageColor(totalUsagePercent)}`}
                style={{ width: `${Math.min(totalUsagePercent, 100)}%` }}
              ></div>
            </div>
          </div>
          <div className={`text-2xl font-bold ${getUsageTextColor(totalUsagePercent)}`}>
            {totalUsagePercent.toFixed(1)}%
          </div>
        </div>
      </div>

      {/* File System Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto scrollbar-thin">
          <table className="w-full">
            <thead className="table-header">
              <tr>
                <th className="px-6 py-3 text-left">Mount Point</th>
                <th className="px-6 py-3 text-left">Device</th>
                <th className="px-6 py-3 text-left">Type</th>
                <th className="px-6 py-3 text-left">Size</th>
                <th className="px-6 py-3 text-left">Used</th>
                <th className="px-6 py-3 text-left">Available</th>
                <th className="px-6 py-3 text-left">Usage</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {data.map((disk, index) => {
                const usagePercent = disk.usedPercent || 0;
                return (
                  <tr key={index} className="table-row">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-2">
                        {getFileSystemIcon(disk.type)}
                        <span className="font-medium text-gray-900 font-mono">
                          {disk.mountPoint}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-gray-700 font-mono text-sm">
                        {disk.name}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700">
                        {disk.type}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-gray-700 font-mono">
                      {disk.totalSpace}
                    </td>
                    <td className="px-6 py-4 text-gray-700 font-mono">
                      {disk.usedSpace}
                    </td>
                    <td className="px-6 py-4 text-gray-700 font-mono">
                      {disk.usableSpace}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3">
                        <div className="flex-1 progress-bar">
                          <div 
                            className={`progress-fill ${getUsageColor(usagePercent)}`}
                            style={{ width: `${Math.min(usagePercent, 100)}%` }}
                          ></div>
                        </div>
                        <span className={`text-sm font-medium ${getUsageTextColor(usagePercent)} min-w-[3rem]`}>
                          {usagePercent.toFixed(1)}%
                        </span>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Storage Distribution Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {data
          .filter(disk => disk.type !== 'tmpfs' && disk.type !== 'proc' && disk.type !== 'sysfs')
          .map((disk, index) => {
            const usagePercent = disk.usedPercent || 0;
            return (
              <div key={index} className="card-compact">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-2">
                    {getFileSystemIcon(disk.type)}
                    <span className="font-medium text-white truncate" title={disk.mountPoint}>
                      {disk.mountPoint}
                    </span>
                  </div>
                  <span className="text-xs text-gray-400 bg-gray-700 px-2 py-1 rounded">
                    {disk.type}
                  </span>
                </div>
                
                <div className="space-y-2">
                  <div className="progress-bar">
                    <div 
                      className={`progress-fill ${getUsageColor(usagePercent)}`}
                      style={{ width: `${Math.min(usagePercent, 100)}%` }}
                    ></div>
                  </div>
                  <div className="flex justify-between text-xs text-gray-400">
                    <span>{disk.usedSpace} used</span>
                    <span className={getUsageTextColor(usagePercent)}>
                      {usagePercent.toFixed(1)}%
                    </span>
                  </div>
                  <div className="text-xs text-gray-500">
                    {disk.usableSpace} available of {disk.totalSpace}
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
};

export default FileSystemTab; 