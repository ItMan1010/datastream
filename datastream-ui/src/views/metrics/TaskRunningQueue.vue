<!--Licensed to the Apache Software Foundation (ASF) under one or more-->
<!--contributor license agreements.  See the NOTICE file distributed with-->
<!--this work for additional information regarding copyright ownership.-->
<!--The ASF licenses this file to You under the Apache License, Version 2.0-->
<!--(the "License"); you may not use this file except in compliance with-->
<!--the License.  You may obtain a copy of the License at-->

<!--http://www.apache.org/licenses/LICENSE-2.0-->

<!--Unless required by applicable law or agreed to in writing, software-->
<!--distributed under the License is distributed on an "AS IS" BASIS,-->
<!--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.-->
<!--See the License for the specific language governing permissions and-->
<!--limitations under the License.-->
<template>
  <div class="task-running-queue-container">
    <div :id="'taskRunningQueue'"
         style="width: 100%; height: 600px; border: 2px solid #e4e7ed; border-radius: 4px;"></div>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import {onMounted, ref, onUnmounted, nextTick} from 'vue';
import constant from '../../comm/constants';
import http from '../../utils/request';
import {ElMessage} from 'element-plus';

// 定义 props
interface Props {
  maxQueueSize?: number;
}

const props = withDefaults(defineProps<Props>(), {
  maxQueueSize: 100
});

let loading = ref(false);

// 存储图表实例
const chartInstance = ref<echarts.ECharts | null>(null);

// 存储API数据的响应式变量
const taskRunningQueueData = ref<any>(null);

const getMonitorTaskRunningQueueApi = async (taskId: number) => {
  // 验证任务ID参数
  if (!taskId || taskId <= 0) {
    console.warn(`无效的任务ID: ${taskId}，跳过API调用`)
    return
  }

  let request = {
    taskId: taskId,
  }

  console.log(`开始获取任务 ${taskId} 的运行队列指标数据`)
  loading.value = true

  try {
    taskRunningQueueData.value = await http(constant.MONITOR_TASK_RUNNING_QUEUE, 'post', request);
    loading.value = false

    if (taskRunningQueueData.value.errorCode !== '0') {
      ElMessage.error(`查询指标出错：${taskRunningQueueData.value.errorMsg}`)
      return
    }

    console.log(`成功获取任务 ${taskId} 的指标数据:`, taskRunningQueueData.value.metricsList?.length || 0, '条记录')

    // 如果父组件传入了 maxQueueSize，更新图表的 y 轴最大值
    if (props.maxQueueSize && props.maxQueueSize > 0) {
      updateChartYAxisMax(props.maxQueueSize);
    }

    // 更新图表数据
    updateChartData(taskRunningQueueData.value.metricsList);
  } catch (err) {
    loading.value = false
    console.error(`获取任务 ${taskId} 指标数据失败:`, err)
    ElMessage.error(`查询指标出错：${err}`)
  }
}

type EChartsOption = echarts.EChartsOption;

onMounted(async () => {
  // 等待DOM渲染完成
  await nextTick();

  // 初始化图表，使用传入的 maxQueueSize 或默认值
  await drawMonitorTaskRunningQueue(props.maxQueueSize);

  // 不在这里自动获取数据，等待父组件调用
  // 移除无效的 getMonitorTaskRunningQueueApi(1) 调用

  // 添加窗口大小变化监听器
  window.addEventListener('resize', handleResize);
})

onUnmounted(() => {
  // 移除窗口大小变化监听器
  window.removeEventListener('resize', handleResize);

  // 销毁图表实例
  if (chartInstance.value) {
    chartInstance.value.dispose();
  }
})

// 处理窗口大小变化
const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize();
  }
}

const drawMonitorTaskRunningQueue = async (yAxisMax) => {
  const chartDom = document.getElementById("taskRunningQueue");
  if (!chartDom) {
    console.error('找不到图表容器元素');
    return;
  }

  // 创建图表实例
  chartInstance.value = echarts.init(chartDom);

  // 设置基础配置
  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: function(params: any) {
        if (params && params.length > 0) {
          const data = params[0];
          const time = new Date(data.value[0]);
          const value = data.value[1];
          const timeStr = time.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
          });
          return `
            <div style="padding: 8px;">
              <div style="font-weight: bold; margin-bottom: 8px; color: var(--primary-color);">
                任务运行队列指标
              </div>
              <div style="margin: 4px 0;">
                <span style="color: #666;">时间：</span>
                <span style="color: #333; font-weight: 500;">${timeStr}</span>
              </div>
              <div style="margin: 4px 0;">
                <span style="color: #666;">队列值：</span>
                <span style="color: var(--primary-color); font-weight: 500; font-size: 16px;">${value}</span>
              </div>
            </div>
          `;
        }
        return '';
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      },
      extraCssText: 'box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1); border-radius: 6px;'
    },
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100,
        zoomLock: false
      },
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 20,
        bottom: 5,
        borderColor: '#e4e7ed',
        fillerColor: 'rgba(64, 158, 255, 0.1)',
        handleStyle: {
          color: '#2563EB'
        }
      }
    ],
    legend: {
      data: ['队列值'],
      top: 5,
      textStyle: {
        color: '#606266',
        fontSize: 12
      }
    },
    grid: {
      left: '8%',
      right: '8%',
      top: '10%',
      bottom: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'time',
      name: '运行时刻',
      axisLabel: {
        formatter: '{HH}:{mm}:{ss}',
        rotate: 45,
        fontSize: 12
      },
      axisLine: {
        show: true
      },
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: yAxisMax,
      name: '通道队列值',
      nameTextStyle: {
        fontSize: 14,
        fontWeight: 'bold'
      },
      axisLabel: {
        fontSize: 12
      },
      splitLine: {
        show: true,
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '队列值',
        data: [],
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: true,
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#2563EB',
            borderColor: '#fff',
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: 'rgba(64, 158, 255, 0.5)'
          }
        },
        lineStyle: {
          color: '#2563EB',
          width: 3
        },
        itemStyle: {
          color: '#2563EB',
          borderColor: '#fff',
          borderWidth: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {offset: 0, color: 'rgba(64, 158, 255, 0.4)'},
              {offset: 1, color: 'rgba(64, 158, 255, 0.1)'}
            ]
          }
        }
      }
    ]
  };

  chartInstance.value.setOption(option);
}

// 解析时间字符串的函数
const parseTimeString = (timeStr: string): number | null => {
  try {
    if (timeStr.length !== 14) {
      console.warn(`时间格式错误: ${timeStr}, 应为14位数字`);
      return null;
    }

    const year = timeStr.substring(0, 4);
    const month = timeStr.substring(4, 6);
    const day = timeStr.substring(6, 8);
    const hour = timeStr.substring(8, 10);
    const minute = timeStr.substring(10, 12);
    const second = timeStr.substring(12, 14);

    // 构造日期字符串 (月份需要减1，因为JS的月份是0-11)
    const date = new Date(
      parseInt(year),
      parseInt(month) - 1,
      parseInt(day),
      parseInt(hour),
      parseInt(minute),
      parseInt(second)
    );

    if (isNaN(date.getTime())) {
      console.warn(`无效的日期: ${timeStr}`);
      return null;
    }

    return date.getTime();
  } catch (error) {
    console.error(`解析时间字符串失败: ${timeStr}`, error);
    return null;
  }
}

// 更新图表 y 轴最大值的函数
const updateChartYAxisMax = (maxValue: number) => {
  if (!chartInstance.value) {
    return;
  }

  const option = {
    yAxis: {
      max: maxValue
    }
  };

  chartInstance.value.setOption(option);
}

// 更新图表数据的函数
const updateChartData = (newData: any) => {
  if (!chartInstance.value || !newData) {
    return;
  }

  let xData: number[] = [];
  let yData: number[] = [];

  // 使用相同的数据解析逻辑
  if (typeof newData === 'object' && Array.isArray(newData)) {
    newData.forEach(item => {
      const timestamp = parseTimeString(String(item.metricsTime));
      if (timestamp) {
        xData.push(timestamp);
        yData.push(Number(item.metricsValue));
      }
    })


    // 按时间排序
    const combined = xData.map((time, index) => ({time, value: yData[index]}));
    combined.sort((a, b) => a.time - b.time);

    xData = combined.map(item => item.time);
    yData = combined.map(item => item.value);
  } else if (newData.xData && newData.yData) {
    xData = newData.xData;
    yData = newData.yData;
  } else if (Array.isArray(newData)) {
    yData = newData;
    xData = yData.map((_, index) => {
      const now = new Date();
      now.setSeconds(now.getSeconds() - (yData.length - index - 1) * 10);
      return now.getTime();
    });
  }

  // 更新图表数据
  const newOption = {
    series: [{
      data: xData.map((time, index) => [time, yData[index]])
    }]
  };

  chartInstance.value.setOption(newOption);
}

// 暴露方法供父组件调用
defineExpose({
  getMonitorTaskRunningQueueApi,
  updateChartData,
  updateChartYAxisMax
})
</script>

<style scoped>
.task-running-queue-container {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  min-height: 700px;
  display: flex;
  flex-direction: column;
}

#taskRunningQueue {
  background: #fff;
  flex: 1;
  min-height: 600px;
}
</style>
