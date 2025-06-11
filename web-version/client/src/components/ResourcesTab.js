import React from 'react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
} from 'chart.js';
import { Line, Pie } from 'react-chartjs-2';
import { Cpu, MemoryStick, HardDrive, Network, Activity } from 'lucide-react';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

const ResourcesTab = ({ data, history }) => {
  if (!data) return <div>Loading resources...</div>;

  const { resources, cpuCores, charts } = data;

  // Prepare historical data for charts
  const prepareHistoryData = (historyData, label, color) => {
    if (!historyData || historyData.length === 0) return { labels: [], datasets: [] };

    const labels = historyData.map((_, index) => {
      const secondsAgo = (historyData.length - 1 - index) * 2;
      return secondsAgo === 0 ? 'Now' : `${secondsAgo}s ago`;
    });

    return {
      labels,
      datasets: [
        {
          label,
          data: historyData.map(item => item.value),
          borderColor: color,
          backgroundColor: color + '20',
          borderWidth: 2,
          fill: true,
          tension: 0.3,
          pointRadius: 0,
          pointHoverRadius: 4,
        }
      ]
    };
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        grid: {
          color: 'rgba(229, 231, 235, 0.8)',
        },
        ticks: {
          color: 'rgba(75, 85, 99, 0.8)',
          maxTicksLimit: 6,
        }
      },
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(229, 231, 235, 0.8)',
        },
        ticks: {
          color: 'rgba(75, 85, 99, 0.8)',
        }
      }
    },
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        titleColor: 'rgb(17, 24, 39)',
        bodyColor: 'rgb(55, 65, 81)',
        borderColor: 'rgba(229, 231, 235, 0.8)',
        borderWidth: 1,
      }
    },
    elements: {
      point: {
        hoverBackgroundColor: 'white',
      }
    }
  };

  const networkChartOptions = {
    ...chartOptions,
    scales: {
      ...chartOptions.scales,
      y: {
        ...chartOptions.scales.y,
        title: {
          display: true,
          text: 'KB/s',
          color: 'rgba(75, 85, 99, 0.8)',
        }
      }
    }
  };

  const getResourceIcon = (name) => {
    switch (name.toLowerCase()) {
      case 'cpu': return <Cpu className="h-5 w-5 text-blue-400" />;
      case 'memory': return <MemoryStick className="h-5 w-5 text-green-400" />;
      case 'swap': return <HardDrive className="h-5 w-5 text-yellow-400" />;
      case 'network up':
      case 'network down': return <Network className="h-5 w-5 text-purple-400" />;
      default: return <Activity className="h-5 w-5 text-gray-400" />;
    }
  };

  const getProgressColor = (percentage) => {
    if (percentage > 90) return 'bg-red-500';
    if (percentage > 75) return 'bg-yellow-500';
    if (percentage > 50) return 'bg-blue-500';
    return 'bg-green-500';
  };

  const createPieData = (used, total, label, colors) => ({
    labels: ['Used', 'Free'],
    datasets: [{
      data: [used, total - used],
      backgroundColor: colors,
      borderColor: '#e5e7eb',
      borderWidth: 2,
    }]
  });

  const pieOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: 'rgb(55, 65, 81)',
          padding: 15,
          usePointStyle: true,
        }
      },
      tooltip: {
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        titleColor: 'rgb(17, 24, 39)',
        bodyColor: 'rgb(55, 65, 81)',
        borderColor: 'rgba(229, 231, 235, 0.8)',
        borderWidth: 1,
      }
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">System Resources</h2>

      {/* Resource Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        {resources.map((resource, index) => (
          <div key={index} className="card-compact">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                {getResourceIcon(resource.name)}
                <span className="font-medium text-gray-900">{resource.name}</span>
              </div>
              <span className="text-sm text-gray-600">{resource.status}</span>
            </div>
            
            {resource.name !== 'Network Up' && resource.name !== 'Network Down' && (
              <>
                <div className="progress-bar mb-2">
                  <div 
                    className={`progress-fill ${getProgressColor(resource.usedPercent)}`}
                    style={{ width: `${Math.min(resource.usedPercent, 100)}%` }}
                  ></div>
                </div>
                <div className="flex justify-between text-xs text-gray-600">
                  <span>{resource.used}</span>
                  <span>{resource.total}</span>
                </div>
              </>
            )}
          </div>
        ))}
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* CPU History Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <Cpu className="h-5 w-5 text-blue-400 mr-2" />
            CPU Usage History
          </h3>
          <div style={{ height: '250px' }}>
            <Line 
              data={prepareHistoryData(history.cpu, 'CPU %', '#3B82F6')}
              options={chartOptions}
            />
          </div>
        </div>

        {/* Memory History Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <MemoryStick className="h-5 w-5 text-green-400 mr-2" />
            Memory Usage History
          </h3>
          <div style={{ height: '250px' }}>
            <Line 
              data={prepareHistoryData(history.memory, 'Memory %', '#10B981')}
              options={chartOptions}
            />
          </div>
        </div>

        {/* Network History Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <Network className="h-5 w-5 text-purple-400 mr-2" />
            Network Activity History
          </h3>
          <div style={{ height: '250px' }}>
            <Line 
              data={{
                labels: history.networkUp?.map((_, index) => {
                  const secondsAgo = (history.networkUp.length - 1 - index) * 2;
                  return secondsAgo === 0 ? 'Now' : `${secondsAgo}s ago`;
                }) || [],
                datasets: [
                  {
                    label: 'Upload',
                    data: history.networkUp?.map(item => item.value) || [],
                    borderColor: '#8B5CF6',
                    backgroundColor: '#8B5CF620',
                    borderWidth: 2,
                    fill: false,
                    tension: 0.3,
                    pointRadius: 0,
                    pointHoverRadius: 4,
                  },
                  {
                    label: 'Download',
                    data: history.networkDown?.map(item => item.value) || [],
                    borderColor: '#06B6D4',
                    backgroundColor: '#06B6D420',
                    borderWidth: 2,
                    fill: false,
                    tension: 0.3,
                    pointRadius: 0,
                    pointHoverRadius: 4,
                  }
                ]
              }}
              options={{
                ...networkChartOptions,
                plugins: {
                  ...networkChartOptions.plugins,
                  legend: {
                    display: true,
                    position: 'top',
                    labels: {
                      color: 'rgb(55, 65, 81)',
                      usePointStyle: true,
                    }
                  }
                }
              }}
            />
          </div>
        </div>

        {/* Memory Usage Pie Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <MemoryStick className="h-5 w-5 text-green-400 mr-2" />
            Memory Distribution
          </h3>
          <div style={{ height: '250px' }}>
            {charts.memory && (
              <Pie 
                data={createPieData(
                  charts.memory, 
                  100, 
                  'Memory',
                  ['#10B981', '#374151']
                )}
                options={pieOptions}
              />
            )}
          </div>
        </div>
      </div>

      {/* CPU Cores Table */}
      {cpuCores && cpuCores.length > 0 && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <Cpu className="h-5 w-5 text-blue-400 mr-2" />
            CPU Core Usage
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {cpuCores.map((core, index) => (
              <div key={index} className="bg-gray-100 rounded-lg p-4">
                <div className="flex justify-between items-center mb-2">
                  <span className="font-medium text-gray-900">{core.name}</span>
                  <span className="text-sm text-gray-700">{core.status}</span>
                </div>
                <div className="progress-bar">
                  <div 
                    className={`progress-fill ${getProgressColor(core.usedPercent)}`}
                    style={{ width: `${Math.min(core.usedPercent, 100)}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ResourcesTab; 