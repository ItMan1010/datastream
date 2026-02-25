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
  <div class="main-content">
    <el-tabs class="md-body">
      <el-tab-pane label="h2数据库">
        <div class="tab-description">
          <div style="margin: 20px 0;">
            <el-button type="info" @click="showIframe = !showIframe" icon="el-icon-view">
              {{ showIframe ? '隐藏' : '显示' }} 内嵌控制台
            </el-button>
          </div>

          <p>
            直接访问路径:
            <code>http://127.0.0.1:9199/datastream/h2</code>
          </p>

          <div v-if="showIframe" style="margin-top:20px; border: 1px solid #ddd;">
            <p style="background: #f5f5f5; padding: 10px; margin: 0;">
              内嵌 H2 控制台（如果显示空白，请使用上方按钮在新窗口打开）
            </p>
            <iframe
              :src="h2Url"
              width="100%"
              height="600"
              frameborder="0"
              @load="onIframeLoad"
            ></iframe>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
export default {
  name: 'H2Manage',
  data() {
    return {
      showIframe: false,
      h2Url: 'http://127.0.0.1:9199/datastream/h2'
    }
  },
  methods: {
    openH2Console() {
      // 在新窗口打开 H2 控制台
      const newWindow = window.open(this.h2Url, '_blank', 'width=1200,height=800,scrollbars=yes,resizable=yes');
      if (!newWindow) {
        this.$message.warning('请允许弹窗，或手动访问: ' + this.h2Url);
      }
    },
    onIframeLoad() {
      console.log('H2 控制台 iframe 加载完成');
    }
  }
}
</script>

<style>
.md-body {
  padding: 10px 20px;
}
</style>
