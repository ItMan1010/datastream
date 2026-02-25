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
let jsPlumbInst = null
let busiConfigCanvasId = null
let busiConfigVueInst = null

// 保存节点信息、连线信息
let rootIdArr = []
let childrenIdArr = []
let flowDefineInfo = {}
let relaObj = {}
let flowNodeId2Id = [] // 保存表中id和页面组件id关系
let initNodeCount = 0
let incrementDistance = 50 // 新增时Y轴位置增加值，保证详情和修改页面，表单不遮挡

/**
 * 生成jsplumb实例
 * @param {vue实例} _this
 * @param {画布id} id
 * @returns
 */
export function getInstance (_this, id) {
  if (jsPlumbInst) {
    return jsPlumbInst
  }
  busiConfigVueInst = _this
  _this.$jsPlumb.importDefaults({
    ConnectionsDetachable: false
  })

  busiConfigCanvasId = id
  jsPlumbInst = _this.$jsPlumb.getInstance({
    Container: id, // 选择器id
    EndpointStyle: { radius: 0.11, fill: '#409EFF' }, // 端点样式
    PaintStyle: { stroke: '#80d4f6', strokeWidth: 2 }, // 绘画样式，默认8px线宽  #456
    HoverPaintStyle: { stroke: '#1E90FF' }, // 默认悬停样式  默认为null
    Connector: ['Bezier'],
    ConnectionOverlays: [
      // 此处可以设置所有箭头的样式，因为我们要改变连接线的样式，故单独配置
      // Arrow-箭头  Label-标签  PlainArrow-平头箭头  Diamond-菱形  Diamond(钻石)-钻石箭头  Custom-自定义
      [
        'Arrow',
        {
          // 设置参数可以参考中文文档
          location: 1,
          length: 8,
          paintStyle: {
            stroke: '#80d4f6',
            fill: '#80d4f6'
          }
        }
      ]
    ],
    ReattachConnections: false
  })
  init(jsPlumbInst)
  return jsPlumbInst
}

// 初始化相关事件
function init (inst) {
  // 开始拖动新连接时触发此事件,将所有端点展示
  inst.bind('beforeDrag', function (info) {
    // busiConfigVueInst.showEndpoint = true
  })
  // 当链接建立
  inst.bind('beforeDrop', function (info) {
    // busiConfigVueInst.showEndpoint = false
    return info.connection.source.id !== info.connection.target.id
  })
  // 双击删除连线
  inst.bind('dblclick', (conn, originalEvent) => {
    // conn.sourceId  conn.targetId
    // busiConfigVueInst.showEndpoint = false
    // 删除连接关系
    inst.deleteConnection(conn)
  })
  scalingCanvas()
}

// 实现画布缩放
function scalingCanvas () {
  // 实现缩放
  let currentScale = 1
  const scale = 1.1 // 缩放因子，可以根据需要调整
  let busiConfigCanvas = $('#' + busiConfigCanvasId).eq(0)
  document.addEventListener('wheel', (e) => {
    if (e.ctrlKey && e.target.id === 'my-canvas') {
      e.preventDefault()
      let delta = e.deltaY // 获取滚轮滚动的方向
      // delta 为正表示向下滚动，为负表示向上滚动
      let zoomFactor = 1.05 // 缩放因子
      if (delta < 0) {
        // 向上滚动，放大
        currentScale *= zoomFactor
      } else {
        // 向下滚动，缩小
        currentScale /= zoomFactor
      }
      busiConfigCanvas.css('transform', 'scale(' + currentScale + ')')
    }
  }, { passive: false }) // 浏览器中某些事件为了性能会使用passive修饰，当监听这些事件时回调函数中e.preventDefault()阻断行为不生效，需要设置传入第三个参数 { passive: false }
}

export const sourceConfig = {
  isSource: true, // 是否可以拖动（作为连线起点）
  isTarget: false, // 是否可以放置（连线终点）
  endpoint: ['Dot', {
    radius: 4,
    fill: 'pink'
  }], // 端点的形状
  connectorStyle: {
    outlineStroke: '#80d4f6',
    strokeWidth: 1
  }, // 连接线的颜色，大小样式
  connectorHoverStyle: { // 连接器鼠标悬停时的样式
    strokeWidth: 2
  },
  paintStyle: {
    fill: '#cecece',
    fillStyle: '#1e8151',
    radius: 6,
    lineWidth: 1
  }, // 端点的颜色样式
  hoverPaintStyle: { stroke: '#409EFF' }, // 鼠标悬停时的样式
  // connector: ["Bezier", { curviness: 80 }],  // 连接线的样式种类有[Bezier],[Flowchart],[StateMachine ],[Straight ]
  connector: ['Flowchart', { stub: [0, 0], gap: 0, cornerRadius: 5, alwaysRespectStubs: true }],
  maxConnections: 20 // 设置连接点最多可以连接几条线
}

export const targetConfig = {
  isSource: true, // 是否可以拖动（作为连线起点）
  isTarget: true, // 是否可以放置（连线终点）
  endpoint: ['Dot', {
    radius: 4,
    fill: 'pink'
  }], // 端点的形状
  connectorStyle: {
    outlineStroke: '#80d4f6',
    strokeWidth: 1
  }, // 连接线的颜色，大小样式
  connectorHoverStyle: { // 连接器鼠标悬停时的样式
    strokeWidth: 2
  },
  paintStyle: {
    fill: '#67C23A', // 端点填充色
    fillStyle: '#1e8151',
    radius: 6, // 半径
    lineWidth: 1
  }, // 端点的颜色样式
  hoverPaintStyle: { stroke: '#409EFF' }, // 鼠标悬停时的样式
  // connector: ["Bezier", { curviness: 80 }],  // 连接线的样式种类有[Bezier],[Flowchart],[StateMachine ],[Straight ]
  connector: ['Flowchart', { stub: [0, 0], gap: 0, cornerRadius: 5, alwaysRespectStubs: true }],
  maxConnections: 20 // 设置连接点最多可以连接几条线
  /* connectorOverlays: [
    ['Arrow', {
      width: 12,
      length: 12,
      location: 1, // 位置百分百
    }]
  ] */
}

/**
 * 记录根节点
 * @param {根节点id} rootId
 */
export function addRootId (rootId) {
  if (!rootIdArr.includes(rootId)) {
    rootIdArr.push(rootId)
  }
}
/**
 * 删除根节点
 * @param {根节点id} rootId
 */
export function delRootId (rootId) {
  rootIdArr = rootIdArr.filter(item => item !== rootId)
}
/**
 * 记录子节点
 * @param {子节点id} childrenId
 */
export function addChildrenId (childrenId) {
  if (!childrenIdArr.includes(childrenId)) {
    childrenIdArr.push(childrenId)
  }
}
/**
 * 删除子节点
 * @param {子节点id} childrenId
 */
export function delChildrenId (childrenId) {
  childrenIdArr = childrenIdArr.filter(item => item != childrenId)
}
/**
 * 重置存储的节点信息
 */
export function resetNodeInfo () {
  jsPlumbInst = null
  busiConfigCanvasId = null
  rootIdArr = []
  childrenIdArr = []
  flowDefineInfo = {}
  relaObj = {}
  flowNodeId2Id = []
}

export function checkFlowDefine (_this) {
  if (rootIdArr.length === 0) {
    // 如果有子节点，必须有根节点
    if (childrenIdArr.length > 0) {
      _this.$message.error(`每条规则至少配置一个根节点！`)
      return false
    } else {
      return true
    }
  }

  // 连线关系
  let connections = jsPlumbInst.getConnections()
  if (connections.length === 0) {
    // 只有一个根节点无需连线
    if (childrenIdArr.length <= 0) {
      return true
    } else if (childrenIdArr.length > 0) { // 有子节点，没有连线
      let nodeDescArr = []
      childrenIdArr.forEach(item => {
        nodeDescArr.push(document.getElementById(item).getAttribute('desc'))
      })
      _this.$message.error(`节点[${nodeDescArr.join(',')}]没有连线，请检查！`)
      return false
    }
  }

  // 从根节点开始找，最后看是否有游离根节点之外的节点
  relaObj = {}
  let allNodeId = []
  getRelationship(connections, rootIdArr[0], relaObj, allNodeId)
  for (let i = 0; i < childrenIdArr.length; i++) {
    let item = childrenIdArr[i]
    if (!allNodeId.includes(item)) {
      let nodeDesc = document.getElementById(item).getAttribute('desc')
      _this.$message.error(`节点[${nodeDesc}]不在根节点所在树中，请检查！`)
      return false
    }
  }
  return true
}
/**
 * 获取所有节点关系
 * @param {*} connections
 * @param {*} rootId
 * @param {*} relaObj
 * @param {*} allNodeId
 */
function getRelationship (connections, rootId, relaObj, allNodeId) {
  relaObj.id = rootId
  relaObj.children = []
  connections.forEach(conn => {
    if (rootId === conn.sourceId) {
      allNodeId.push(conn.targetId)
      let child = {}
      getRelationship(connections, conn.targetId, child, allNodeId)
      relaObj.children.push(child)
    }
  })
}
/**
 * 新增流程表单信息
 * @param {*} id
 * @param {*} form
 */
export function addFlowDefineInfo (id, form) {
  flowDefineInfo[id] = form
}
/**
 * 删除流程表单信息
 * @param {*} id
 * @param {*} form
 */
export function delFlowDefineInfo (id) {
  delete flowDefineInfo[id]
}
/**
 * 获取所有流程表单信息
 * @returns
 */
export function getFlowDefineInfo () {
  return flowDefineInfo
}

export function saveFlowNodeId2Id (flowNodeId, id, parentFlowNodeId) {
  flowNodeId2Id.push({
    flowNodeId,
    id,
    parentFlowNodeId
  })
  initConnect()
}

export function checkFlowData (_this) {
  let nodeDescArr = []
  rootIdArr.forEach(item => {
    if (!flowDefineInfo[item]) {
      nodeDescArr.push(document.getElementById(item).getAttribute('desc'))
    }
  })
  childrenIdArr.forEach(item => {
    if (!flowDefineInfo[item]) {
      nodeDescArr.push(document.getElementById(item).getAttribute('desc'))
    }
  })
  if (nodeDescArr.length > 0) {
    _this.$message.error(`节点[${nodeDescArr.join(',')}]信息没有配置，请检查！`)
    return false
  }
  return true
}
export function buildTableLinkNode (mode) {
  let tableLinkNode = {}
  // 没有连接关系
  if (!relaObj || !relaObj.id) {
    // 没有根节点
    if (rootIdArr.length === 0) {
      return null
    } else { // 组装根节点信息
      let posX = parseInt(document.getElementById(rootIdArr[0]).style.left.replace('px', ''))
      let posY = parseInt(document.getElementById(rootIdArr[0]).style.top.replace('px', ''))
      tableLinkNode = {
        tableName: flowDefineInfo[rootIdArr[0]].tableName,
        fieldName: flowDefineInfo[rootIdArr[0]].fieldName,
        parentFieldName: flowDefineInfo[rootIdArr[0]].parentFieldName,
        posX: posX,
        posY: mode === 'add' ? posY + incrementDistance : posY
      }
      return tableLinkNode
    }
  }
  return getCanalFlowNodeByShip(tableLinkNode, relaObj, mode)
}

function getCanalFlowNodeByShip (canalFlowNode, relaObjX, mode) {
  if (relaObjX.id) {
    let posX = parseInt(document.getElementById(relaObjX.id).style.left.replace('px', ''))
    let posY = parseInt(document.getElementById(relaObjX.id).style.top.replace('px', ''))
    canalFlowNode = {
      tableName: flowDefineInfo[relaObjX.id].tableName,
      fieldName: flowDefineInfo[relaObjX.id].fieldName,
      parentFieldName: flowDefineInfo[relaObjX.id].parentFieldName,
      posX: posX,
      posY: mode === 'add' ? posY + incrementDistance : posY
    }
    if (relaObjX.children && relaObjX.children.length > 0) {
      canalFlowNode.linkNodeList = []
      relaObjX.children.forEach(item => {
        canalFlowNode.linkNodeList.push(getCanalFlowNodeByShip({}, item, mode))
      })
    }
    return canalFlowNode
  }
}
/**
 * 初始化节点数
 * @param {*} count
 */
export function setInitNodeCount (count) {
  initNodeCount = count
}
/**
 * 连续初始化
 * @returns
 */
function initConnect () {
  if (flowNodeId2Id.length < initNodeCount) {
    return
  }
  for (let i = 0; i < flowNodeId2Id.length; i++) {
    for (let j = 0; j < flowNodeId2Id.length; j++) {
      if (flowNodeId2Id[i].parentFlowNodeId === flowNodeId2Id[j].flowNodeId) {
        jsPlumbInst.connect({
          uuids: [flowNodeId2Id[j].id + 'out', flowNodeId2Id[i].id + 'in']
        })
        break
      }
    }
  }
}
