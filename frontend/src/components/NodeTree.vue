<template>
  <div class="node-tree-container">
    <div class="tree-header">
      <h3 class="text-lg font-bold text-cyan-400">OPC-UA 节点树</h3>
      <el-tag :type="store.isConnected ? 'success' : 'danger'" size="small">
        {{ store.isConnected ? '已连接' : '未连接' }}
      </el-tag>
    </div>

    <el-tree
      :data="store.nodeTree"
      :props="treeProps"
      node-key="id"
      highlight-current
      default-expand-all
      @node-click="handleNodeClick"
      class="dark-tree"
    >
      <template #default="{ node, data }">
        <span class="custom-tree-node">
          <el-icon v-if="data.type === 'Object'" class="text-yellow-400">
            <Folder />
          </el-icon>
          <el-icon v-else-if="data.type === 'Variable'" class="text-green-400">
            <DataLine />
          </el-icon>
          <span class="node-label">{{ data.name }}</span>
          <el-tag
            v-if="data.type === 'Variable' && data.quality"
            :type="data.quality === 'Good' ? 'success' : data.quality === 'Bad' ? 'danger' : 'warning'"
            size="small"
            class="ml-2"
          >
            {{ data.quality }}
          </el-tag>
          <span v-if="data.type === 'Variable' && data.value !== undefined" class="node-value">
            {{ data.value }}{{ data.unit ? ' ' + data.unit : '' }}
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 节点详情面板 -->
    <div v-if="store.selectedNode" class="node-detail-panel">
      <el-divider />
      <h4 class="text-sm font-bold text-cyan-300 mb-2">节点详情</h4>
      <el-descriptions :column="1" size="small" border class="dark-descriptions">
        <el-descriptions-item label="名称">
          <span class="text-white">{{ store.selectedNode.name }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="Node ID">
          <span class="font-mono text-slate-300 text-xs">{{ store.selectedNode.nodeId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="getNodeTypeTagType(store.selectedNode.type)" size="small" effect="dark">
            {{ getNodeTypeLabel(store.selectedNode.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据类型">
          <template v-if="store.selectedNode.dataType">
            <el-tag type="info" size="small" effect="plain">{{ store.selectedNode.dataType }}</el-tag>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="单位">
          <template v-if="store.selectedNode.unit">
            <span class="text-cyan-300 font-mono">{{ store.selectedNode.unit }}</span>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="当前值">
          <template v-if="store.selectedNode.value !== undefined">
            <span class="text-green-400 font-mono font-semibold">
              {{ formatValue(store.selectedNode.value, store.selectedNode.dataType) }}{{ store.selectedNode.unit ? ' ' + store.selectedNode.unit : '' }}
            </span>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="质量码">
          <template v-if="store.selectedNode.quality">
            <el-tag
              :type="getQualityTagType(store.selectedNode.quality)"
              size="small"
              effect="dark"
            >
              {{ store.selectedNode.quality }}
            </el-tag>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          <template v-if="store.selectedNode.timestamp">
            <span class="text-slate-300 text-xs font-mono">{{ formatTimestamp(store.selectedNode.timestamp) }}</span>
            <span class="ml-1 text-slate-500 text-xs">({{ getTimeAgo(store.selectedNode.timestamp) }})</span>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item label="最近质量变化">
          <template v-if="store.selectedNode.lastQualityChange">
            <div class="flex items-center gap-1 flex-wrap">
              <el-tag
                :type="getQualityTagType(store.selectedNode.lastQualityChange.from)"
                size="small"
                effect="plain"
              >
                {{ store.selectedNode.lastQualityChange.from }}
              </el-tag>
              <el-icon class="text-slate-400" :size="14"><ArrowRight /></el-icon>
              <el-tag
                :type="getQualityTagType(store.selectedNode.lastQualityChange.to)"
                size="small"
                effect="dark"
              >
                {{ store.selectedNode.lastQualityChange.to }}
              </el-tag>
              <div class="w-full mt-1 text-slate-500 text-xs font-mono">
                {{ formatTimestamp(store.selectedNode.lastQualityChange.timestamp) }}
              </div>
            </div>
          </template>
          <span v-else class="text-slate-500 text-xs">暂无变化记录</span>
        </el-descriptions-item>
        <el-descriptions-item label="描述">
          <template v-if="store.selectedNode.description">
            <span class="text-slate-300">{{ store.selectedNode.description }}</span>
          </template>
          <span v-else class="text-slate-500 text-xs">—</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="store.selectedNode.browseName" label="浏览名称">
          <span class="text-slate-300 font-mono text-xs">{{ store.selectedNode.browseName }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="mt-3 flex gap-2">
        <el-button
          v-if="store.selectedNode.type === 'Variable'"
          type="primary"
          size="small"
          @click="handleSubscribe"
        >
          {{ isSubscribed ? '取消订阅' : '订阅' }}
        </el-button>
        <el-button
          v-if="store.selectedNode.type === 'Variable'"
          type="info"
          size="small"
          @click="handleReadValue"
        >
          读取
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Folder, DataLine, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useOpcuaStore } from '../store/opcua'
import type { OPCUANode } from '../types'

const store = useOpcuaStore()

const treeProps = {
  children: 'children',
  label: 'name'
}

const isSubscribed = computed(() => {
  if (!store.selectedNode) return false
  return store.subscriptions.has(store.selectedNode.id)
})

function handleNodeClick(data: OPCUANode) {
  store.selectNode(data)
}

function handleSubscribe() {
  if (!store.selectedNode) return
  if (isSubscribed.value) {
    store.removeSubscription(store.selectedNode.id)
    ElMessage.success(`已取消订阅: ${store.selectedNode.name}`)
  } else {
    store.addSubscription(store.selectedNode.id)
    ElMessage.success(`已订阅: ${store.selectedNode.name}`)
  }
}

function handleReadValue() {
  if (!store.selectedNode) return
  const valueStr = formatValue(store.selectedNode.value, store.selectedNode.dataType)
  ElMessage.success(`${store.selectedNode.name} = ${valueStr} ${store.selectedNode.unit || ''}`)
}

function formatTimestamp(timestamp: number): string {
  const date = new Date(timestamp)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function getTimeAgo(timestamp: number): string {
  const diff = Date.now() - timestamp
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  if (seconds > 0) return `${seconds}秒前`
  return '刚刚'
}

function getQualityTagType(quality: string): 'success' | 'warning' | 'danger' {
  if (quality === 'Good') return 'success'
  if (quality === 'Uncertain') return 'warning'
  return 'danger'
}

function getNodeTypeTagType(type: string): 'primary' | 'success' | 'warning' | 'info' {
  switch (type) {
    case 'Object': return 'warning'
    case 'Variable': return 'success'
    case 'Method': return 'primary'
    case 'DataType': return 'info'
    default: return 'info'
  }
}

function getNodeTypeLabel(type: string): string {
  switch (type) {
    case 'Object': return '对象'
    case 'Variable': return '变量'
    case 'Method': return '方法'
    case 'DataType': return '数据类型'
    default: return type
  }
}

function formatValue(value: any, dataType?: string): string {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'boolean') return value ? 'TRUE' : 'FALSE'
  if (dataType === 'Double' && typeof value === 'number') {
    return value.toFixed(2)
  }
  return String(value)
}
</script>

<style scoped>
.node-tree-container {
  height: 100%;
  overflow-y: auto;
  padding: 12px;
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  flex: 1;
  overflow: hidden;
}

.node-label {
  white-space: nowrap;
}

.node-value {
  margin-left: auto;
  font-family: monospace;
  font-size: 12px;
  color: #67c23a;
  padding-left: 8px;
}

.node-detail-panel {
  padding: 8px 0;
}

:deep(.el-tree) {
  background: transparent !important;
  color: #e0e0e0 !important;
}

:deep(.el-tree-node__content:hover) {
  background: rgba(6, 182, 212, 0.1) !important;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: rgba(6, 182, 212, 0.2) !important;
}

:deep(.el-descriptions) {
  --el-descriptions-item-bordered-label-background: #1f2937;
}
</style>
