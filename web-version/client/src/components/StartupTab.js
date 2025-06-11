import React, { useState } from 'react';
import { Settings, Terminal, Clock, Filter } from 'lucide-react';

const StartupTab = ({ data }) => {
  const [filter, setFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');

  if (!data || data.length === 0) {
    return (
      <div className="space-y-6">
        <h2 className="text-2xl font-bold text-gray-900">Startup Applications</h2>
        <div className="card text-center py-12">
          <Settings className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500">No startup applications found</p>
        </div>
      </div>
    );
  }

  const getTypeIcon = (type) => {
    switch (type?.toLowerCase()) {
      case 'systemd':
        return <Settings className="h-5 w-5 text-blue-400" />;
      case 'cron':
        return <Clock className="h-5 w-5 text-green-400" />;
      case 'rc.local':
        return <Terminal className="h-5 w-5 text-yellow-400" />;
      default:
        return <Settings className="h-5 w-5 text-gray-400" />;
    }
  };

  const getTypeBadgeColor = (type) => {
    switch (type?.toLowerCase()) {
      case 'systemd':
        return 'bg-blue-900 text-blue-300 border-blue-700';
      case 'cron':
        return 'bg-green-900 text-green-300 border-green-700';
      case 'rc.local':
        return 'bg-yellow-900 text-yellow-300 border-yellow-700';
      default:
        return 'bg-gray-700 text-gray-300 border-gray-600';
    }
  };

  // Filter and categorize data
  const filteredData = data.filter(item => {
    const matchesSearch = item.name.toLowerCase().includes(filter.toLowerCase()) ||
                         item.path.toLowerCase().includes(filter.toLowerCase());
    const matchesType = typeFilter === 'all' || item.type === typeFilter;
    return matchesSearch && matchesType;
  });

  // Group by type
  const groupedData = filteredData.reduce((acc, item) => {
    const type = item.type || 'unknown';
    if (!acc[type]) {
      acc[type] = [];
    }
    acc[type].push(item);
    return acc;
  }, {});

  const types = [...new Set(data.map(item => item.type || 'unknown'))];
  const typeCounts = types.map(type => ({
    type,
    count: data.filter(item => (item.type || 'unknown') === type).length
  }));

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold text-gray-900">Startup Applications</h2>
        <div className="text-sm text-gray-600">
          {filteredData.length} of {data.length} items
        </div>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search startup applications..."
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="w-full px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div className="flex items-center space-x-2">
            <Filter className="h-4 w-4 text-gray-400" />
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-2 bg-white border border-gray-300 rounded-lg text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">All Types</option>
              {types.map(type => (
                <option key={type} value={type}>
                  {type.charAt(0).toUpperCase() + type.slice(1)}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Type Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {typeCounts.map(({ type, count }) => (
          <div key={type} className="card-compact">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                {getTypeIcon(type)}
                <span className="font-medium text-white capitalize">{type}</span>
              </div>
              <span className="text-2xl font-bold text-gray-300">{count}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Startup Items by Type */}
      {Object.entries(groupedData).map(([type, items]) => (
        <div key={type} className="card">
          <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
            {getTypeIcon(type)}
            <span className="ml-2 capitalize">{type} Services</span>
            <span className="ml-2 text-sm text-gray-400">({items.length})</span>
          </h3>
          
          <div className="space-y-3">
            {items.map((item, index) => (
              <div 
                key={index} 
                className="bg-gray-700 rounded-lg p-4 border border-gray-600 hover:border-gray-500 transition-colors duration-200"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-3 mb-2">
                      <h4 className="font-medium text-white truncate" title={item.name}>
                        {item.name}
                      </h4>
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getTypeBadgeColor(type)}`}>
                        {type}
                      </span>
                    </div>
                    
                    <div className="text-sm text-gray-300 break-all">
                      <span className="text-gray-400">Path: </span>
                      <code className="bg-gray-800 px-2 py-1 rounded text-xs font-mono">
                        {item.path}
                      </code>
                    </div>
                  </div>
                </div>

                {/* Additional info based on type */}
                {type === 'systemd' && (
                  <div className="mt-3 pt-3 border-t border-gray-600">
                    <div className="flex items-center space-x-4 text-xs text-gray-400">
                      <span>Service: {item.name.endsWith('.service') ? 'Yes' : 'No'}</span>
                      <span>Auto-start: Enabled</span>
                    </div>
                  </div>
                )}

                {type === 'cron' && (
                  <div className="mt-3 pt-3 border-t border-gray-600">
                    <div className="flex items-center space-x-4 text-xs text-gray-400">
                      <span>Schedule: @reboot</span>
                      <span>User: Current user</span>
                    </div>
                  </div>
                )}

                {type === 'rc.local' && (
                  <div className="mt-3 pt-3 border-t border-gray-600">
                    <div className="flex items-center space-x-4 text-xs text-gray-400">
                      <span>Execution: At system boot</span>
                      <span>Priority: Last</span>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      ))}

      {/* Empty state when filtered */}
      {filteredData.length === 0 && data.length > 0 && (
        <div className="card text-center py-12">
          <Filter className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-400">No items match your current filters</p>
          <button 
            onClick={() => {
              setFilter('');
              setTypeFilter('all');
            }}
            className="btn-primary mt-4"
          >
            Clear Filters
          </button>
        </div>
      )}
    </div>
  );
};

export default StartupTab; 