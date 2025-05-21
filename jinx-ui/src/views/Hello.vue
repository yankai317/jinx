<template>
  <div class="hello">
    <h1>{{ message }}</h1>
    <div class="input-container">
      <input 
        v-model="inputContent" 
        type="text" 
        placeholder="请输入内容" 
        class="input-field"
      />
      <button @click="saveContent" class="submit-btn">保存</button>
    </div>
  </div>
</template>

<script>
/* eslint-disable */
import axios from 'axios'

export default {
  name: 'HelloApp',
  data() {
    return {
      message: '加载中...',
      inputContent: ''
    }
  },
  mounted() {
    this.fetchHello()
  },
  methods: {
    fetchHello() {
      axios.get('/hello')
        .then(response => {
          this.message = response.data
        })
        .catch(error => {
          console.error('获取数据失败:', error)
          this.message = '获取数据失败，请稍后再试'
        })
    },
    saveContent() {
      if (!this.inputContent.trim()) {
        alert('请输入内容')
        return
      }
      
      axios.post('/save', this.inputContent)
        .then(response => {
          alert('保存成功')
          this.inputContent = ''
          this.fetchHello() // 刷新显示
        })
        .catch(error => {
          console.error('保存失败:', error)
          alert('保存失败，请稍后再试')
        })
    }
  }
}
</script>

<style scoped>
.hello {
  margin: 40px auto;
  max-width: 800px;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  background-color: #f9f9f9;
}

h1 {
  color: #3a8ee6;
}

.input-container {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.input-field {
  flex: 1;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
}

.submit-btn {
  padding: 10px 20px;
  background-color: #3a8ee6;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.submit-btn:hover {
  background-color: #66b1ff;
}
</style> 
