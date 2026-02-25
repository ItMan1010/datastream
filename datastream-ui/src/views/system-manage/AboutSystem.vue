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
      <el-tab-pane label="系统介绍">
        <div v-html="renderedHtmlMD" class="markdown-content"></div>
      </el-tab-pane>
<!--      <el-tab-pane label="功能介绍">-->
<!--        <div>-->
<!--          <video ref="videoPlayer" muted autoplay controls style="border-radius: 10px;width: 650px;" @ended="restartVideo"  >-->
<!--            <source :src="sourceSrc" type="video/mp4">-->
<!--            浏览器不支持.-->
<!--          </video>-->
<!--          <div>-->
<!--            <span>功能介绍.mp4</span>-->
<!--          </div>-->
<!--        </div>-->
<!--      </el-tab-pane>-->
<!--      <el-tab-pane label="版本发布">-->
<!--        <div v-html="renderedChangesHtmlMD" class="markdown-content"></div>-->
<!--      </el-tab-pane>-->
    </el-tabs>
  </div>
</template>
<script>
import http from '@/utils/request.js'
import { marked } from 'marked'

export default {
  name: 'AboutTheSystem',
  data() {
    return {
      readmeMD: '',
      sourceSrc: '',
      changesHtmlMD: '',
    }
  },
  computed: {
    renderedHtmlMD() {
      return this.readmeMD ? marked(this.readmeMD) : ''
    },
    renderedChangesHtmlMD() {
      return this.changesHtmlMD ? marked(this.changesHtmlMD) : ''
    }
  },
  mounted() {
    const markdownPath = '/static/README.md'
    const changesPath = '/static/CHANGES.md'
    http(markdownPath, 'get')
      .then(markdown => {
        markdown = markdown.replaceAll('./images/', '/datastream/static/images/')
        this.readmeMD = markdown
      })
      .catch(error => {
        console.error('Error loading markdown:', error);
      });
    http(changesPath, 'get')
      .then(markdown => {
        this.changesHtmlMD = markdown
      })
      .catch(error => {
        console.error('Error loading changesMarkdown:', error);
      });
    if (process.env.NODE_ENV !== 'production') {
      this.sourceSrc = '/static/datastream.mp4'
    } else {
      this.sourceSrc = '/datastream/static/datastream.mp4'
    }
  },
  methods: {
    restartVideo() {
      // 当视频结束时，将当前播放时间设置为0，实现重新开始
      this.$refs.videoPlayer.currentTime = 0;
      // 播放视频
      this.$refs.videoPlayer.play();
    }
  }
}
</script>
<style>
.md-body {
  padding: 10px 20px;
}
.md-body img {
  max-width: 850px !important;
}
.markdown-content {
  line-height: 1.6;
}
.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4,
.markdown-content h5,
.markdown-content h6 {
  margin-top: 24px;
  margin-bottom: 16px;
  font-weight: 600;
  line-height: 1.25;
}
.markdown-content p {
  margin-bottom: 16px;
}
.markdown-content code {
  padding: 0.2em 0.4em;
  margin: 0;
  font-size: 85%;
  background-color: rgba(27,31,35,0.05);
  border-radius: 3px;
}
.markdown-content pre {
  padding: 16px;
  overflow: auto;
  font-size: 85%;
  line-height: 1.45;
  background-color: #f6f8fa;
  border-radius: 3px;
}
.markdown-content blockquote {
  padding: 0 1em;
  color: #6a737d;
  border-left: 0.25em solid #dfe2e5;
  margin: 0 0 16px 0;
}
</style>
