import React, { useState, useMemo } from 'react';
import { X, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';

const ProcessTable = ({ processes, onKillProcess }) => {
  const [sortField, setSortField] = useState('cpuValue');
  const [sortDirection, setSortDirection] = useState('desc');
  const [filter, setFilter] = useState('');

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const filteredAndSortedProcesses = useMemo(() => {
    let filtered = processes.filter(process =>
      process.name.toLowerCase().includes(filter.toLowerCase()) ||
      process.user.toLowerCase().includes(filter.toLowerCase()) ||
      process.pid.includes(filter)
    );

    return filtered.sort((a, b) => {
      const aVal = a[sortField];
      const bVal = b[sortField];
      
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
      }
      
      const aStr = String(aVal).toLowerCase();
      const bStr = String(bVal).toLowerCase();
      
      if (sortDirection === 'asc') {
        return aStr.localeCompare(bStr);
      } else {
        return bStr.localeCompare(aStr);
      }
    });
  }, [processes, sortField, sortDirection, filter]);

  const getSortIcon = (field) => {
    if (sortField !== field) {
      return <ArrowUpDown className="h-4 w-4 text-gray-400" />;
    }
    return sortDirection === 'asc' 
      ? <ArrowUp className="h-4 w-4 text-blue-400" />
      : <ArrowDown className="h-4 w-4 text-blue-400" />;
  };

  const getProgressBarColor = (percentage) => {
    if (percentage > 80) return 'bg-red-500';
    if (percentage > 60) return 'bg-yellow-500';
    if (percentage > 40) return 'bg-blue-500';
    return 'bg-green-500';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Running Processes</h2>
        <div className="flex items-center space-x-4">
          <input
            type="text"
            placeholder="Filter processes..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
          <span className="text-gray-600 text-sm">
            {filteredAndSortedProcesses.length} of {processes.length} processes
          </span>
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto scrollbar-thin" style={{ maxHeight: '600px' }}>
          <table className="w-full">
            <thead className="table-header sticky top-0">
              <tr>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('name')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>Process</span>
                    {getSortIcon('name')}
                  </button>
                </th>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('user')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>User</span>
                    {getSortIcon('user')}
                  </button>
                </th>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('pid')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>PID</span>
                    {getSortIcon('pid')}
                  </button>
                </th>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('cpuValue')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>CPU %</span>
                    {getSortIcon('cpuValue')}
                  </button>
                </th>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('rssValue')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>Memory (MB)</span>
                    {getSortIcon('rssValue')}
                  </button>
                </th>
                <th className="px-6 py-3 text-left">
                  <button 
                    onClick={() => handleSort('virtualMemValue')}
                    className="flex items-center space-x-1 hover:text-gray-900"
                  >
                    <span>Virtual (MB)</span>
                    {getSortIcon('virtualMemValue')}
                  </button>
                </th>
                <th className="px-6 py-3 text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredAndSortedProcesses.map((process) => (
                <tr key={process.pid} className="table-row">
                  <td className="px-6 py-4">
                    <div className="font-medium text-gray-900 truncate max-w-xs" title={process.name}>
                      {process.name}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-700">{process.user}</td>
                  <td className="px-6 py-4 text-gray-700 font-mono">{process.pid}</td>
                  <td className="px-6 py-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-gray-700 font-mono min-w-[3rem]">{process.cpu}%</span>
                      <div className="flex-1 progress-bar">
                        <div 
                          className={`progress-fill ${getProgressBarColor(process.cpuValue)}`}
                          style={{ width: `${Math.min(process.cpuValue, 100)}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center space-x-2">
                      <span className="text-gray-700 font-mono min-w-[4rem]">{process.rss}</span>
                      <div className="flex-1 progress-bar">
                        <div 
                          className={`progress-fill ${getProgressBarColor(Math.min(process.rssValue / 10, 100))}`}
                          style={{ width: `${Math.min(process.rssValue / 10, 100)}%` }}
                        ></div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-700 font-mono">{process.virtualMem}</td>
                  <td className="px-6 py-4 text-center">
                    <button
                      onClick={() => {
                        if (window.confirm(`Are you sure you want to kill process ${process.name} (PID: ${process.pid})?`)) {
                          onKillProcess(process.pid);
                        }
                      }}
                      className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors duration-200"
                      title="Kill Process"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ProcessTable; 