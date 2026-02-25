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
  <div :id="chartId" class="chart-container" :style="{ height: height + 'px' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: {
    type: String,
    default: '资源趋势图'
  },
  data: {
    type: Array,
    default: () => []
  },
  type: {
    type: String,
    default: 'line'
  },
  height: {
    type: Number,
    default: 300
  },
  unit: {
    type: String,
    default: ''
  }
})

const chartId = ref(`chart-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`)
let chartInstance = null

// 初始化图表
const initChart = () => {
  const chartDom = document.getElementById(chartId.value)
  if (!chartDom) {
    return
  }

  chartInstance = echarts.init(chartDom)

  updateChart()
}

// 更新图表
const updateChart = () => {
  if (!chartInstance) {
    return
  }

  const times = props.data.map(item => item.time)
  const values = props.data.map(item => item.value)

  // 动态计算渐变色
  const getGradientColor = () => {
    return {
      type: 'linear',
      x: 0,
      y: 0,
      x2: 0,
      y2: 1,
      colorStops: [
        { offset: 0, color: 'rgba(64, 158, 255, 0.5)' },
        { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
      ]
    }
  }

  const option = {
    title: {
      text: props.title,
      left: 12,
      top: 8,
      textStyle: {
        fontSize: 14,
        fontWeight: 600,
        color: '#303133'
      }
    },
    grid: {
      left: '12%',
      right: '4%',
      top: '50',
      bottom: '12%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      borderColor: 'rgba(0, 0, 0, 0.8)',
      textStyle: {
        color: '#fff',
        fontSize: 12
      },
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: 'rgba(64, 158, 255, 0.3)',
          type: 'dashed'
        }
      },
      formatter: (params) => {
        if (!params || params.length === 0) return ''
        const item = params[0]
        return `${item.axisValue}<br/>${item.marker}${item.seriesName}: <b>${item.value}</b> ${props.unit}`
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times,
      axisLine: {
        lineStyle: {
          color: '#EBEEF5'
        }
      },
      axisLabel: {
        color: '#909399',
        fontSize: 11
      },
      splitLine: {
        show: false
      }
    },
    yAxis: {
      type: 'value',
      name: props.unit,
      nameTextStyle: {
        color: '#909399',
        fontSize: 11,
        padding: [0, 0, 0, -8]
      },
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#909399',
        fontSize: 11
      },
      splitLine: {
        lineStyle: {
          color: '#EBEEF5',
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: props.title,
        type: props.type,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: values,
        areaStyle: props.type === 'line' ? getGradientColor() : undefined,
        lineStyle: {
          width: 2.5,
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF',
          borderColor: '#fff',
          borderWidth: 2
        },
        emphasis: {
          focus: 'series',
          itemStyle: {
            color: '#409EFF',
            borderColor: '#fff',
            borderWidth: 3,
            shadowBlur: 10,
            shadowColor: 'rgba(64, 158, 255, 0.5)'
          }
        }
      }
    ]
  }

  chartInstance.setOption(option, true)
}

// 监听数据变化
watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

// 窗口大小变化时调整图表
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', handleResize)
  })
})

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chart-container {
  width: 100%;
}
</style>
