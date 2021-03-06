<template>
  <div>
    <Row v-if="fixed">
      <i-input v-model="inputValue" search @on-keyup="inputOnSearch" @on-search="inputOnSearch" @on-focus="inputFocus" />
      <u-tree :tree="treeData" :size="size" :collapse="collapse" :async="async" :loadingText="loadingText" :klass="klass"
              :maxHeight="maxHeight"
              @item-click="handlerItemClick"
              :sort="sort" :ref="refName"></u-tree>
    </Row>
    <Row v-else>
      <Dropdown trigger="custom" style="width:100%" :visible="visible" class="utree-select" @on-clickoutside="handleClose">
        <i-input v-model="inputValue" search @on-keyup="inputOnSearch" @on-search="inputOnSearch" @on-focus="inputFocus"/>
        <div   slot="list">
          <u-tree v-model="selectId" :tree="treeData" :size="size" :collapse="collapse" :async="async" :loadingText="loadingText" :klass="klass"
                  :maxHeight="maxHeight"
                  @item-click="handlerItemClick"
                  :sort="sort" :ref="refName"></u-tree>
        </div>
      </Dropdown>
    </Row>
  </div>
</template>
<style>
  /* 指定样式下的dropdown宽度设置为100%，避免受iview组件影响 */
  .utree-select .ivu-dropdown-rel {
    width: 100%;
  }
</style>

<script type="text/jsx">
import UTree from '../tree/UTree'
import deepExtend from 'deep-extend'
// 自增长ID值，防止一个页面2个以上的treeselect组件，导致refName重名
import treeId from './treeid'

export default {
  name: 'UTreeSelect',
  componentName: 'UTreeSelect',
  props: {
    // 是否固定显示，固定则默认带搜索框并全部展示
    fixed: {
      type: Boolean,
      default: false
    },
    // 组件使用v-model即可获取返回值
    value: {
      type: [String, Number],
      default: ''
    },
    tree: {
      type: Array,
      default () {
        return []
      }
    },
    size: {type: String, validator: value => ['large', 'small'].indexOf(value) > -1},
    collapse: {type: Boolean, default: false},
    async: {type: Function},
    loadingText: {type: String, default: 'Loading...'},
    klass: String,
    maxHeight: {
      type: Number,
      default: 400
    },
    // 排序字段 {sort:"desc|asc",field:"要排序的字段"}
    sort: {
      type: Object,
      default: null
    },
    refName: {
      type: String,
      default: 'tree' + (treeId.getId())
    }
  },
  components: {
    UTree
  },

  data () {
    return {
      // 传给UTree组件的数据
      treeData: [],
      inputValue: null,
      selectTreeItem: null,
      visible: false,
      // 组件的宽度，用于控制搜索结果展示时的长度
      treeWidth: null,
      // 当前选择的节点ID，也可以当初始值赋值给utree组件
      selectId: null,
      opt: {
        showCheckbox: false,
        multiple: false,
        allowBatch: false,
        wholeRow: true
      }
    }
  },

  // 监听父节点的值发生变化后，动态修改内部的数据
  watch: {
    value: function (newValue, oldValue) {
      if (newValue !== oldValue) {
        let name = ''
        for (let o of this.treeData) {
          if (o['id'] === parseInt(newValue)) {
            name = o['name']
            break
          }
        }
        this.selectId = parseInt(newValue)
        this.inputValue = name
      }
    },
    tree: function (newValue, oldValue) {
      this.treeData = newValue
    },
    inputValue: function (newValue, oldValue) {
      // 如果没有选择任何内容，则树结构恢复原始状态
      if (newValue === '' || newValue === null) {
        // 避免污染this.tree
        this.$nextTick(() => {
          this.treeData = [...this.tree]
        })
      }
    }
  },
  created () {
    // 避免污染this.tree
    this.treeData = [...this.tree]
    this.selectId = this.value
  },
  methods: {
    inputOnSearch (event) {
      this.$emit('input', null)
      if (this.inputValue !== '' && this.inputValue !== null) {
        this.treeData = this.searchTree(this.tree, this.inputValue)
      } else {
        this.treeData = [...this.tree]
      }
    },
    // 通过构造一个新的列表方式来展示搜索结果，不直接渲染树
    searchTree (tree, keyWord) {
      let newTree = []
      // 从原始的数据当中生成path路径，进行搜索
      tree.forEach((t, index) => {
        if (t.path.indexOf(keyWord) > -1) {
          // 新复制对象，避免污染
          let newItem = deepExtend({}, t)
          let reg = new RegExp('(' + keyWord + ')', 'g')
          let labelStr = t.path
          let label = [...t.path]
          let charWidth = 12
          // 每个字符占12px
          let width = label.length * charWidth
          // 超过组件宽度时，需要截取字符串(从后往前）
          if (width > this.treeWidth) {
            let m = (width % charWidth === 0) ? width / charWidth : parseInt(width / charWidth)
            let n = (this.treeWidth % charWidth === 0) ? this.treeWidth / charWidth : parseInt(this.treeWidth / charWidth)
            m = Math.min(n - 8, m)
            let strs = []
            label.reverse().forEach((c, index) => {
              if (index < m) {
                strs.push(c)
              }
            })
            strs.push('...')
            labelStr = strs.reverse().join('')
          }
          newItem.tip = t.path
          newItem.label = labelStr.replace(reg, '<font color=red>$1</font>')
          // 干掉所有的ParentId。不按层级显示，全部打平
          newItem.parentId = null
          // 最多显示50个
          if (index < 50) {
            newTree.push(newItem)
          }
        }
      })
      return newTree
    },

    inputFocus () {
      this.visible = true
      this.$nextTick(() => {
        this.treeWidth = this.$refs[this.refName].$el.offsetWidth
      })
    },
    handlerItemClick (node, item, e) {
      this.visible = false
      this.selectId = parseInt(item.id)
      // 更新v-model属性的值
      this.$emit('input', parseInt(item.id))
      this.inputValue = item.name
      this.$emit('item-click', node, item, e)
      // 调用一下input的blur事件，用于触发表单校验
        this.$forceUpdate()
    },
    handleClose () {
      this.visible = false
    },
    asyncLoadData (oriNode, callback) {
      let data = []
      if (oriNode.value) {

      } else {
        // 查找第一层数据
        data.push(oriNode.treeData)
      }
      callback(data)
    }
  },
  mounted () {
    this.$nextTick(() => {
      this.treeWidth = this.$refs[this.refName].$el.offsetWidth
    })
  }

}
</script>
