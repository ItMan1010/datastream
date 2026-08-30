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
  <div class="main-content overview-page">
    <!-- 任务维度统计区域 -->
    <el-row :gutter="16" class="dimension-cards">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="dimension-card" shadow="never">
          <div class="dimension-title">
            <el-icon><Histogram /></el-icon>
            <span>任务类型分布</span>
          </div>
          <div id="taskTypeChart" class="dimension-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12">
        <el-card class="dimension-card" shadow="never">
          <div class="dimension-title">
            <el-icon><PieChart /></el-icon>
            <span>任务状态分布</span>
          </div>
          <div id="taskStateChart" class="dimension-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-divider />

    <!-- 图表控制栏 -->
    <div class="chart-control">
      <div class="chart-title">
        <el-icon><TrendCharts /></el-icon>
        <span>任务统计趋势</span>
      </div>
      <el-select v-model.number="cycle" @change="changeCycle" class="cycle-select">
        <el-option label="1日内" :value="1" />
        <el-option label="3日内" :value="3" />
        <el-option label="7日内" :value="7" />
        <el-option label="30日内" :value="30" />
      </el-select>
    </div>

    <!-- 图表区域 -->
    <el-card class="chart-card" shadow="never">
      <div id="myChart" class="chart-container"></div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="overview-cards">
      <el-col :span="24 / 5">
        <el-card class="stat-card" shadow="hover" @click="gotoDetail(2)">
          <div class="card-content">
            <div class="card-left">
              <div class="card-icon" style="background: linear-gradient(135deg, #35A3F6 0%, #2D8FE3 100%)">
                <el-icon :size="32"><DocumentCopy /></el-icon>
              </div>
            </div>
            <div class="card-right">
              <div class="card-value">{{ overviewInfo.moveTaskSumCount }}</div>
              <div class="card-label">迁移任务总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24 / 5">
        <el-card class="stat-card" shadow="hover" @click="gotoDetail(2)">
          <div class="card-content">
            <div class="card-left">
              <div class="card-icon" style="background: linear-gradient(135deg, #F6B352 0%, #F0A030 100%)">
                <el-icon :size="32"><Clock /></el-icon>
              </div>
            </div>
            <div class="card-right">
              <div class="card-value">{{ overviewInfo.moveTaskRunCount }}</div>
              <div class="card-label">迁移执行数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24 / 5">
        <el-card class="stat-card" shadow="hover" @click="gotoDetail(3)">
          <div class="card-content">
            <div class="card-left">
              <div class="card-icon" style="background: linear-gradient(135deg, #EF546C 0%, #E63E59 100%)">
                <el-icon :size="32"><Link /></el-icon>
              </div>
            </div>
            <div class="card-right">
              <div class="card-value">{{ overviewInfo.linkTaskSumCount }}</div>
              <div class="card-label">表链接任务总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24 / 5">
        <el-card class="stat-card" shadow="hover" @click="gotoDetail(3)">
          <div class="card-content">
            <div class="card-left">
              <div class="card-icon" style="background: linear-gradient(135deg, #91CC75 0%, #7DB85E 100%)">
                <el-icon :size="32"><VideoPlay /></el-icon>
              </div>
            </div>
            <div class="card-right">
              <div class="card-value">{{ overviewInfo.linkTaskRunCount }}</div>
              <div class="card-label">表链接任务执行数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="24 / 5">
        <el-card class="stat-card" shadow="hover" @click="gotoDetail(4)">
          <div class="card-content">
            <div class="card-left">
              <div class="card-icon" style="background: linear-gradient(135deg, #41C9C5 0%, #36B4B0 100%)">
                <el-icon :size="32"><Connection /></el-icon>
              </div>
            </div>
            <div class="card-right">
              <div class="card-value">{{ overviewInfo.dataSourceCount }}</div>
              <div class="card-label">数据源连接数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { onMounted, onActivated, onDeactivated, onBeforeUnmount, getCurrentInstance, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Connection, DocumentCopy, Clock, Link, VideoPlay, TrendCharts, Histogram, PieChart } from '@element-plus/icons-vue'
import { useOverview } from '@/composables/useOverview'
import { useEventBus } from '@/composables/useEventBus'
import { useMainStore } from '@/store/index.js'

export default {
  name: 'OverView',
  components: {
    Connection,
    DocumentCopy,
    Clock,
    Link,
    VideoPlay,
    TrendCharts,
    Histogram,
    PieChart
  },
  setup() {
    const instance = getCurrentInstance()
    const route = useRoute()
    const mainStore = useMainStore()
    const overview = useOverview()
    const { on, emit } = useEventBus()

    // 获取echarts实例
    const getEcharts = () => {
      return instance?.appContext.config.globalProperties.$echarts
    }

    // 初始化全部图表（类型图/状态图先初始化，趋势图最后初始化并触发一次数据加载）
    const initCharts = () => {
      const echarts = getEcharts()
      if (!echarts) return false

      const trendDom = document.getElementById('myChart')
      const typeDom = document.getElementById('taskTypeChart')
      const stateDom = document.getElementById('taskStateChart')

      if (typeDom) overview.initTaskTypeChart(typeDom, echarts)
      if (stateDom) overview.initTaskStateChart(stateDom, echarts)
      if (trendDom) overview.initChart(trendDom, echarts)

      return true
    }

    // 初始化
    onMounted(() => {
      nextTick(() => {
        if (!initCharts()) {
          setTimeout(() => {
            initCharts()
          }, 200)
        }
      })

      // 监听图表更新事件
      on('changeChart', () => {
        if (route.name !== 'overview') return
        setTimeout(() => {
          overview.queryCanalInfo(route.name, instance?.proxy?.$el)
        }, 500)
      })
    })

    onActivated(() => {
      nextTick(() => {
        if (!overview.myChart.value || overview.myChart.value.isDisposed()) {
          if (!initCharts()) {
            setTimeout(() => {
              initCharts()
            }, 200)
          }
        } else {
          overview.resizeChart()
          setTimeout(() => {
            overview.queryCanalInfo(route.name, instance?.proxy?.$el)
          }, 300)
        }
      })
    })

    onDeactivated(() => {
      // 组件停用时的处理
    })

    onBeforeUnmount(() => {
      overview.disposeChart()
    })

    // 跳转详情并更新store
    const gotoDetail = (pageIdx) => {
      const queryParams = overview.gotoDetail(pageIdx)
      if (queryParams) {
        mainStore.commQueryParams = queryParams
      }
      emit('gotoPage', pageIdx)
    }

    return {
      cycle: overview.cycle,
      overviewInfo: overview.overviewInfo,
      changeCycle: overview.changeCycle,
      gotoDetail
    }
  }
}
</script>

<style scoped>
.overview-page {
  background: transparent;
}

/* 统计卡片 */
.overview-cards {
  margin-top: 16px;
}

.stat-card {
  height: 120px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
  height: 100%;
}

.card-content {
  display: flex;
  height: 100%;
  align-items: center;
}

.card-left {
  flex-shrink: 0;
  margin-right: 16px;
}

.card-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  transition: all 0.3s ease;
}

.stat-card:hover .card-icon {
  transform: scale(1.1);
}

.card-right {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  margin-bottom: 4px;
}

.card-label {
  font-size: 14px;
  color: #909399;
}

/* 图表控制栏 */
.chart-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chart-title .el-icon {
  color: var(--primary-color);
}

.cycle-select {
  width: 130px;
}

/* 图表卡片 */
.chart-card :deep(.el-card__body) {
  padding: 20px;
}

.chart-container {
  width: 100%;
  height: 450px;
}

/* 任务维度统计卡片 */
.dimension-card :deep(.el-card__body) {
  padding: 20px;
}

.dimension-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.dimension-title .el-icon {
  color: var(--primary-color);
}

.dimension-chart {
  width: 100%;
  height: 320px;
}
</style>
