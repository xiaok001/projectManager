/**
 * 表格列 show-overflow-tooltip 公共配置
 * 统一 popper-class 指向 global.css 中的 .pm-tooltip
 * 用法: <el-table-column show-overflow-tooltip :tooltip-options="tooltipOpt" />
 */
export const tooltipOpt = { popperClass: 'pm-tooltip' }

/**
 * 便捷写法：直接用于 show-overflow-tooltip 属性
 * 用法: <el-table-column v-bind="overflowTooltip" />
 */
export const overflowTooltip = {
  'show-overflow-tooltip': { popperClass: 'pm-tooltip' },
}
