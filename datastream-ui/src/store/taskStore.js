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
// stores/taskStore.js
import { defineStore } from 'pinia'

export const useTaskStore = defineStore('task', {
  state: () => ({
    loading: false,
    queryForm: {
      taskId: '',
      queryFlag: '4',
      tableName: '',
      state: '0',
      moveDate: [],
      batchTaskId: '',
      copyTaskId: null,
      taskType: '1',
    },
    pageNum: 1,
    pageSize: 50,
    dataMoveTaskListTotal: 0,
    dataMoveTaskListData: [],
    refreshChecked: false,
    expandedRows: new Set(),
    // 其他状态...
  }),
  actions: {
    async queryDataMoveTaskList() {
      // 查询逻辑...
    },
    // 其他操作方法...
  },
  getters: {
    pickerOptions: () => ({
      shortcuts: [
        {
          text: '今天',
          value: () => {
            const start = new Date()
            start.setHours(0, 0, 0, 0)
            const end = new Date()
            end.setHours(23, 59, 59, 999)
            return [start, end]
          }
        },
        // 其他快捷选项...
      ]
    })
  }
})
