/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 系统概览业务逻辑Hook
 */
import { ref, reactive, onMounted, onActivated, onDeactivated, onBeforeUnmount, getCurrentInstance } from 'vue'
import http from '@/utils/request.js'
import constant from '@/comm/constants.js'
import * as commMethod from '@/comm/commMethod.js'
import { useMessage } from './useMessage'
import { useEventBus } from './useEventBus'
import dayjs from 'dayjs'

export function useOverview() {
  const { showError } = useMessage()
  const { on, emit } = useEventBus()

  // 图表实例
  const myChart = ref(null)

  // 周期选择
  const cycle = ref(30)

  // 概览信息
  const overviewInfo = reactive({
    dataSourceCount: 0,
    moveTaskSumCount: 0,
    moveTaskRunCount: 0,
    linkTaskSumCount: 0,
    linkTaskRunCount: 0
  })

  // 初始化图表
  const initChart = (chartDom, echarts) => {
    try {
      if (chartDom && echarts && !myChart.value) {
        if (myChart.value) {
          myChart.value.dispose()
          myChart.value = null
        }
        myChart.value = echarts.init(chartDom)
        queryCanalInfo()
      } else if (myChart.value) {
        queryCanalInfo()
      }
    } catch (error) {
      console.error('initChart失败:', error)
      myChart.value = null
    }
  }

  // 查询概览信息
  const queryCanalInfo = async (routeName, el) => {
    // 检查是否在概览页面
    if (routeName && routeName !== 'overview') {
      return
    }

    // 检查组件是否在视图中
    if (el && (!el || !el.parentNode)) {
      return
    }

    // 检查图表容器
    const chartDom = document.getElementById('myChart')
    if (!chartDom) {
      return
    }

    // 检查图表实例
    if (!myChart.value || myChart.value.isDisposed()) {
      return
    }

    try {
      const res = await http(constant.STAT_SYSTEM_INFO, 'post', { days: cycle.value })

      if (res.errorCode !== '0') {
        showError(`查询概览信息失败：${res.errorMsg}`)
        return
      }

      const statSystemInfoEntity = res.statSystemInfoEntity || {}

      Object.assign(overviewInfo, {
        dataSourceCount: statSystemInfoEntity.dataSourceCount,
        moveTaskSumCount: statSystemInfoEntity.moveTaskSumCount,
        moveTaskRunCount: statSystemInfoEntity.moveTaskRunCount,
        linkTaskSumCount: statSystemInfoEntity.linkTaskSumCount,
        linkTaskRunCount: statSystemInfoEntity.linkTaskRunCount
      })

      const moveTaskDayCountList = statSystemInfoEntity.moveTaskDayCountList || []
      const linkTaskDayCountList = statSystemInfoEntity.linkTaskDayCountList || []

      const countInfo = {}

      moveTaskDayCountList.forEach(item => {
        countInfo[item.taskDate] = {
          moveCount: item.taskCount,
          linkCount: 0
        }
      })

      linkTaskDayCountList.forEach(item => {
        if (countInfo[item.taskDate]) {
          countInfo[item.taskDate].linkCount = item.taskCount
        } else {
          countInfo[item.taskDate] = {
            moveCount: 0,
            linkCount: item.taskCount
          }
        }
      })

      createChart(countInfo)
    } catch (err) {
      console.error('查询概览信息失败:', err)
    }
  }

  // 创建图表
  const createChart = (countInfo) => {
    if (!myChart.value || myChart.value.isDisposed()) {
      return
    }

    const entries = Object.entries(countInfo)
    entries.sort((a, b) => new Date(a[0]) - new Date(b[0]))
    const sortedMap = new Map(entries)

    const moveCountList = []
    const linkCountList = []
    const dateList = getData(sortedMap, moveCountList, linkCountList)

    const options = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderColor: 'rgba(0, 0, 0, 0.8)',
        textStyle: {
          color: '#fff',
          fontSize: 12
        },
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['迁移任务数', '链接任务数'],
        top: 8,
        right: 16,
        textStyle: {
          fontSize: 13,
          color: '#606266'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        top: '60',
        bottom: '8%',
        containLabel: true
      },
      xAxis: {
        data: dateList,
        axisLine: {
          lineStyle: {
            color: '#EBEEF5'
          }
        },
        axisLabel: {
          color: '#909399',
          fontSize: 11
        },
        axisTick: {
          show: false
        }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: {
          lineStyle: {
            color: '#EBEEF5',
            type: 'dashed'
          }
        },
        minInterval: 1,
        axisLabel: {
          color: '#909399',
          fontSize: 11,
          formatter: '{value}'
        }
      },
      series: [
        {
          name: '迁移任务数',
          type: 'bar',
          data: moveCountList,
          barMaxWidth: 40,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#35A3F6' },
                { offset: 1, color: '#2D8FE3' }
              ]
            },
            borderRadius: [4, 4, 0, 0]
          },
          emphasis: {
            itemStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: '#4DB3FF' },
                  { offset: 1, color: '#35A3F6' }
                ]
              }
            }
          }
        },
        {
          name: '链接任务数',
          type: 'bar',
          data: linkCountList,
          barMaxWidth: 40,
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#91CC75' },
                { offset: 1, color: '#7DB85E' }
              ]
            },
            borderRadius: [4, 4, 0, 0]
          },
          emphasis: {
            itemStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  { offset: 0, color: '#A5D98F' },
                  { offset: 1, color: '#91CC75' }
                ]
              }
            }
          }
        }
      ]
    }

    myChart.value.setOption(options)
  }

  // 获取图表数据
  const getData = (countInfo, moveCountList, linkCountList) => {
    const data = []

    for (const [key, value] of countInfo) {
      data.push(key)
      moveCountList.push(value.moveCount || 0)
      linkCountList.push(value.linkCount || 0)
    }

    if (data.length === 0) {
      data.push(commMethod.dateFormat(new Date(), 'YYYY-MM-DD'))
      moveCountList.push(0)
      linkCountList.push(0)
    }

    return data
  }

  // 切换周期
  const changeCycle = () => {
    queryCanalInfo()
  }

  // 跳转详情
  const gotoDetail = (pageIdx) => {
    let queryParams = null

    if (pageIdx === 4) {
      queryParams = { dataBaseType: '0' }
    } else if (pageIdx === 2) {
      queryParams = {
        queryFlag: '4',
        moveDate: getDateRange(cycle.value)
      }
    } else if (pageIdx === 3) {
      queryParams = {
        queryFlag: '4',
        backDate: getDateRange(cycle.value)
      }
    }

    // 触发页面跳转
    emit('gotoPage', pageIdx)
    return queryParams
  }

  // 获取日期范围
  const getDateRange = (numOfDays) => {
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * (numOfDays - 1))
    start.setHours(0, 0, 0, 0)
    const end = new Date()
    end.setHours(23, 59, 59, 999)
    // 返回字符串格式，与 el-date-picker 的 value-format 保持一致
    return [
      dayjs(start).format('YYYY-MM-DD HH:mm:ss'),
      dayjs(end).format('YYYY-MM-DD HH:mm:ss')
    ]
  }

  // 调整图表大小
  const resizeChart = () => {
    if (myChart.value && !myChart.value.isDisposed()) {
      myChart.value.resize()
    }
  }

  // 清理图表
  const disposeChart = () => {
    if (myChart.value && !myChart.value.isDisposed()) {
      try {
        myChart.value.dispose()
      } catch (error) {
        console.error('清理图表实例失败:', error)
      }
    }
    myChart.value = null
  }

  return {
    myChart,
    cycle,
    overviewInfo,
    initChart,
    queryCanalInfo,
    createChart,
    changeCycle,
    gotoDetail,
    getDateRange,
    resizeChart,
    disposeChart
  }
}

