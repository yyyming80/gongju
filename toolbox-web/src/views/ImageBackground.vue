<template>
  <div class="background-container">
    <div class="header">
      <h1>证件照换底色</h1>
      <p class="subtitle">AI智能抠图 · 高清无损 · 完全免费</p>
    </div>

    <div class="content">
      <div class="upload-section">
        <div class="upload-area" :class="{ 'has-image': imageFile }" @click="chooseFile">
          <input ref="fileInput" type="file" accept="image/*" @change="handleFileChange" style="display: none">
          <div v-if="!imageFile" class="upload-placeholder">
            <div class="icon">📷</div>
            <p>点击或拖拽上传图片</p>
            <p class="hint">支持 JPG、PNG、JPEG 格式</p>
          </div>
          <img v-else :src="previewUrl" alt="预览">
        </div>
      </div>

      <div class="control-section">
        <div class="color-selector">
          <h3>选择背景颜色</h3>
          <div class="color-options">
            <div 
              v-for="color in colors" 
              :key="color.value"
              class="color-option"
              :class="{ active: selectedColor === color.value }"
              :style="{ backgroundColor: color.hex }"
              @click="selectColor(color.value)"
            >
              <span v-if="selectedColor === color.value" class="check">✓</span>
              <span class="color-name">{{ color.name }}</span>
            </div>
          </div>
        </div>

        <button 
          class="process-btn" 
          :disabled="!imageFile || loading"
          @click="processImage"
        >
          {{ loading ? '处理中...' : '开始处理' }}
        </button>
      </div>

      <div class="result-section" v-if="resultImage">
        <h3>处理结果</h3>
        <img :src="resultImage" alt="结果">
        <div class="actions">
          <button class="download-btn" @click="downloadImage">下载图片</button>
          <button class="reset-btn" @click="reset">重新上传</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ImageBackground',
  data() {
    return {
      imageFile: null,
      previewUrl: '',
      selectedColor: 'blue',
      loading: false,
      resultImage: ''
    };
  },
  computed: {
    colors() {
      return [
        { name: '白色', value: 'white', hex: '#ffffff' },
        { name: '蓝色', value: 'blue', hex: '#4385f5' },
        { name: '红色', value: 'red', hex: '#ff4b4b' }
      ];
    }
  },
  methods: {
    chooseFile() {
      this.$refs.fileInput.click();
    },
    handleFileChange(event) {
      const file = event.target.files[0];
      if (file) {
        this.imageFile = file;
        this.previewUrl = URL.createObjectURL(file);
        this.resultImage = '';
      }
    },
    selectColor(color) {
      this.selectedColor = color;
    },
    async processImage() {
      if (!this.imageFile) return;
      
      this.loading = true;
      try {
        const formData = new FormData();
        formData.append('file', this.imageFile);
        formData.append('color', this.selectedColor);
        
        const response = await axios.post('http://localhost:8080/api/image/background', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        
        if (response.data.code === 200 && response.data.data) {
          this.resultImage = response.data.data.fileUrl;
        } else {
          alert('处理失败: ' + (response.data.msg || '未知错误'));
        }
      } catch (error) {
        console.error('处理失败:', error);
        alert('处理失败，请稍后重试');
      } finally {
        this.loading = false;
      }
    },
    downloadImage() {
      if (!this.resultImage) return;
      
      const link = document.createElement('a');
      link.href = this.resultImage;
      link.download = 'id-photo.png';
      link.click();
    },
    reset() {
      this.imageFile = null;
      this.previewUrl = '';
      this.resultImage = '';
      this.selectedColor = 'blue';
    }
  }
};
</script>

<style scoped>
.background-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

.content {
  display: grid;
  gap: 30px;
}

.upload-section {
  width: 100%;
}

.upload-area {
  width: 100%;
  height: 400px;
  border: 2px dashed #ddd;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
  background: #f9f9f9;
}

.upload-area:hover {
  border-color: #4385f5;
  background: #f0f7ff;
}

.upload-placeholder {
  text-align: center;
  color: #999;
}

.upload-placeholder .icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.upload-placeholder .hint {
  font-size: 12px;
  margin-top: 8px;
  color: #bbb;
}

.upload-area.has-image img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.control-section {
  background: #fff;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.color-selector h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 15px;
}

.color-options {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
}

.color-option {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 3px solid transparent;
  transition: all 0.3s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
  position: relative;
}

.color-option:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.color-option.active {
  border-color: #4385f5;
}

.color-option .check {
  position: absolute;
  top: 5px;
  right: 5px;
  background: #4385f5;
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.color-option .color-name {
  position: absolute;
  bottom: -25px;
  font-size: 12px;
  color: #666;
}

.process-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.process-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.process-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.result-section {
  background: #fff;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  text-align: center;
}

.result-section h3 {
  font-size: 18px;
  color: #333;
  margin-bottom: 20px;
}

.result-section img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 15px;
  justify-content: center;
}

.download-btn, .reset-btn {
  padding: 12px 30px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.download-btn {
  background: #4385f5;
  color: white;
}

.download-btn:hover {
  background: #3367cc;
}

.reset-btn {
  background: #f0f0f0;
  color: #333;
}

.reset-btn:hover {
  background: #e0e0e0;
}
</style>
