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
  <span :class="{ 'running-animate': isRunning }" :style="{ color: stateColor }">{{ stateText }}</span>
</template>

<script>
/**
 * 任务状态文本组件
 * 显示带颜色的任务状态文本
 * 运行中状态具有动态闪烁效果
 */
import { computed } from 'vue'
import { getTaskStateInfo } from '@/constants/taskConstants'

export default {
  name: 'TaskStateText',
  props: {
    state: {
      type: [Number, String],
      required: true
    }
  },
  setup(props) {
    const stateInfo = computed(() => {
      return getTaskStateInfo(Number(props.state))
    })

    const stateText = computed(() => stateInfo.value.text)
    const stateColor = computed(() => stateInfo.value.color)
    const isRunning = computed(() => stateInfo.value.animate === true)

    return {
      stateText,
      stateColor,
      isRunning
    }
  }
}
</script>

<style scoped>
.running-animate {
  animation: pulse-green 1.5s ease-in-out infinite;
}

@keyframes pulse-green {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
