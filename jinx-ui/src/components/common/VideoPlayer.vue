
<template>
  <div class="video-player">
    <video
      ref="videoElement"
      class="video-element"
      :poster="coverImage"
      @timeupdate="onTimeUpdate"
      @ended="onVideoEnded"
      @play="onVideoPlay"
      @pause="onVideoPause"
      @loadedmetadata="onVideoLoaded"
      @click="togglePlay"
    >
      <source :src="videoUrl" type="video/mp4">
      您的浏览器不支持 HTML5 视频播放。
    </video>
    
    <div class="video-controls">
      <div class="progress-container">
        <div class="progress-bar" @click="seekVideo">
          <div class="progress-filled" :style="{ width: `${progressPercentage}%` }"></div>
        </div>
        <div class="time-display">
          {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
        </div>
      </div>
      <div class="control-buttons">
        <button class="control-button" @click="togglePlay">
          <svg v-if="isPlaying" width="16" height="21" viewBox="0 0 16 21" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3.04167 18.0985Q2.04167 18.6727 1.04167 18.1395Q0.0416667 17.5653 0 16.4169L0 1.97938Q0.0416667 0.830946 1.04167 0.256727Q2.04167 -0.276476 3.04167 0.297743L15.0417 7.51649Q15.9583 8.09071 16 9.19813Q15.9583 10.2645 15.0417 10.8798L3.04167 18.0985Z" fill="white"/>
          </svg>
          <svg v-else width="16" height="21" viewBox="0 0 16 21" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3.04167 18.0985Q2.04167 18.6727 1.04167 18.1395Q0.0416667 17.5653 0 16.4169L0 1.97938Q0.0416667 0.830946 1.04167 0.256727Q2.04167 -0.276476 3.04167 0.297743L15.0417 7.51649Q15.9583 8.09071 16 9.19813Q15.9583 10.2645 15.0417 10.8798L3.04167 18.0985Z" fill="white"/>
          </svg>
        </button>
        <div class="playback-rate" @click="togglePlaybackRateMenu">
          <span>{{ playbackRate }}×</span>
          <div class="playback-rate-menu" v-if="showPlaybackRateMenu">
            <div 
              v-for="rate in playbackRates" 
              :key="rate" 
              class="playback-rate-item"
              :class="{ active: playbackRate === rate }"
              @click.stop="setPlaybackRate(rate)"
            >
              {{ rate }}×
            </div>
          </div>
        </div>
        
        <div class="volume-control">
          <button class="control-button" @click="toggleMute">
            <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M15.3394 0.148059C15.8488 0.397839 16.1727 0.923405 16.1727 1.50001C16.1727 1.50001 16.1727 22.5 16.1727 22.5C16.1727 23.0766 15.8488 23.6022 15.3394 23.8519C14.83 24.1017 14.2254 24.0315 13.784 23.6713C13.784 23.6713 6.83551 18 6.83551 18C6.83551 18 1.47025 18 1.47025 18C0.658252 18 0 17.3284 0 16.5C0 16.5 0 7.50001 0 7.50001C0 6.67157 0.658252 6.00001 1.47025 6.00001C1.47025 6.00001 6.83551 6.00001 6.83551 6.00001C6.83551 6.00001 13.784 0.328711 13.784 0.328711C14.2254 -0.0314931 14.83 -0.101721 15.3394 0.148059C15.3394 0.148059 15.3394 0.148059 15.3394 0.148059ZM13.2322 4.62094C13.2322 4.62094 8.26969 8.67131 8.26969 8.67131C8.009 8.88408 7.68509 9 7.35124 9C7.35124 9 2.9405 9 2.9405 9C2.9405 9 2.9405 15 2.9405 15C2.9405 15 7.35124 15 7.35124 15C7.68509 15 8.009 15.1159 8.26969 15.3286C8.26969 15.3286 13.2322 19.3791 13.2322 19.3791C13.2322 19.3791 13.2322 4.62094 13.2322 4.62094C13.2322 4.62094 13.2322 4.62094 13.2322 4.62094ZM20.3379 5.62919C20.9122 5.04349 21.843 5.04364 22.4172 5.62951C24.0709 7.31726 25 9.60602 25 11.9925C25 14.379 24.0709 16.6677 22.4172 18.3555C21.843 18.9414 20.9122 18.9415 20.3379 18.3558C19.7637 17.7702 19.7635 16.8204 20.3376 16.2345C21.4402 15.1093 22.0595 13.5835 22.0595 11.9925C22.0595 10.4015 21.4402 8.87567 20.3376 7.75051C19.7635 7.16464 19.7637 6.21488 20.3379 5.62919C20.3379 5.62919 20.3379 5.62919 20.3379 5.62919Z" fill="white"/>
            </svg>
          </button>
          <div class="volume-slider-container">
            <input 
              type="range" 
              min="0" 
              max="100" 
              :value="volume * 100" 
              class="volume-slider" 
              @input="onVolumeChange"
            >
          </div>
        </div>
        
        <button class="control-button" @click="toggleFullscreen">
          <svg width="21" height="21" viewBox="0 0 21 21" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M1.5 21Q0.84375 21 0.421875 20.5781Q0 20.1562 0 19.5L0 15Q0 14.3438 0.421875 13.9219Q0.84375 13.5 1.5 13.5Q2.15625 13.5 2.57812 13.9219Q3 14.3438 3 15L3 18L6 18Q6.65625 18 7.07812 18.4219Q7.5 18.8438 7.5 19.5Q7.5 20.1562 7.07812 20.5781Q6.65625 21 6 21L1.5 21ZM3 6Q3 6.65625 2.57812 7.07812Q2.15625 7.5 1.5 7.5Q0.84375 7.5 0.421875 7.07812Q0 6.65625 0 6L0 1.5Q0 0.84375 0.421875 0.421875Q0.84375 0 1.5 0L6 0Q6.65625 0 7.07812 0.421875Q7.5 0.84375 7.5 1.5Q7.5 2.15625 7.07812 2.57812Q6.65625 3 6 3L3 3L3 6ZM15 21Q14.3438 21 13.9219 20.5781Q13.5 20.1562 13.5 19.5Q13.5 18.8438 13.9219 18.4219Q14.3438 18 15 18L18 18L18 15Q18 14.3438 18.4219 13.9219Q18.8437 13.5 19.5 13.5Q20.1562 13.5 20.5781 13.9219Q21 14.3438 21 15L21 19.5Q21 20.1562 20.5781 20.5781Q20.1562 21 19.5 21L15 21ZM21 6Q21 6.65625 20.5781 7.07812Q20.1562 7.5 19.5 7.5Q18.8437 7.5 18.4219 7.07812Q18 6.65625 18 6L18 3L15 3Q14.3438 3 13.9219 2.57812Q13.5 2.15625 13.5 1.5Q13.5 0.84375 13.9219 0.421875Q14.3438 0 15 0L19.5 0Q20.1562 0 20.5781 0.421875Q21 0.84375 21 1.5L21 6Z" fill="white"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VideoPlayer',
  props: {
    videoUrl: {
      type: String,
      required: true
    },
    coverImage: {
      type: String,
      default: ''
    },
    lastPosition: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      isPlaying: false,
      isMuted: false,
      volume: 1,
      currentTime: 0,
      duration: 0,
      progressPercentage: 0,
      isFullscreen: false,
      playbackRate: 1,
      playbackRates: [0.5, 0.75, 1, 1.25, 1.5, 2],
      showPlaybackRateMenu: false,
      progressReportInterval: null
    }
  },
  mounted() {
    // 初始化视频播放器
    this.initPlayer();
    
    // 监听键盘事件
    window.addEventListener('keydown', this.handleKeyDown);
    
    // 监听全屏变化事件
    document.addEventListener('fullscreenchange', this.onFullscreenChange);
    document.addEventListener('webkitfullscreenchange', this.onFullscreenChange);
    document.addEventListener('mozfullscreenchange', this.onFullscreenChange);
    document.addEventListener('MSFullscreenChange', this.onFullscreenChange);
    
    // 点击其他地方关闭倍速菜单
    document.addEventListener('click', this.closePlaybackRateMenu);
  },
  beforeUnmount() {
    // 移除事件监听
    window.removeEventListener('keydown', this.handleKeyDown);
    document.removeEventListener('fullscreenchange', this.onFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', this.onFullscreenChange);
    document.removeEventListener('mozfullscreenchange', this.onFullscreenChange);
    document.removeEventListener('MSFullscreenChange', this.onFullscreenChange);
    document.removeEventListener('click', this.closePlaybackRateMenu);
    
    // 清除定时器
    if (this.progressReportInterval) {
      clearInterval(this.progressReportInterval);
    }
  },
  methods: {
    // 初始化播放器
    initPlayer() {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      // 设置音量
      video.volume = this.volume;
      
      // 如果有上次播放位置，设置播放位置
      if (this.lastPosition > 0) {
        video.currentTime = this.lastPosition;
      }
      
      // 设置进度报告定时器，每10秒报告一次进度
      this.progressReportInterval = setInterval(() => {
        this.$emit('progress-change', this.currentTime);
      }, 10000);
    },
    
    // 播放/暂停切换
    togglePlay() {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      if (video.paused) {
        video.play();
      } else {
        video.pause();
      }
    },
    
    // 静音切换
    toggleMute() {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      video.muted = !video.muted;
      this.isMuted = video.muted;
    },
    
    // 全屏切换
    toggleFullscreen() {
      const videoContainer = this.$el;
      if (!this.isFullscreen) {
        if (videoContainer.requestFullscreen) {
          videoContainer.requestFullscreen();
        } else if (videoContainer.mozRequestFullScreen) {
          videoContainer.mozRequestFullScreen();
        } else if (videoContainer.webkitRequestFullscreen) {
          videoContainer.webkitRequestFullscreen();
        } else if (videoContainer.msRequestFullscreen) {
          videoContainer.msRequestFullscreen();
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen();
        }
      }
    },
    
    // 全屏状态变化处理
    onFullscreenChange() {
      this.isFullscreen = !!(
        document.fullscreenElement ||
        document.webkitFullscreenElement ||
        document.mozFullScreenElement ||
        document.msFullscreenElement
      );
    },
    
    // 时间格式化
    formatTime(seconds) {
      seconds = Math.floor(seconds);
      const minutes = Math.floor(seconds / 60);
      const remainingSeconds = seconds % 60;
      return `${minutes}:${remainingSeconds < 10 ? '0' : ''}${remainingSeconds}`;
    },
    
    // 音量变化处理
    onVolumeChange(event) {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      this.volume = event.target.value / 100;
      video.volume = this.volume;
      
      // 如果音量大于0，取消静音
      if (this.volume > 0) {
        video.muted = false;
        this.isMuted = false;
      } else {
        video.muted = true;
        this.isMuted = true;
      }
    },
    
    // 视频时间更新处理
    onTimeUpdate() {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      this.currentTime = video.currentTime;
      this.duration = video.duration || 0;
      
      // 计算进度百分比
      if (this.duration > 0) {
        this.progressPercentage = (this.currentTime / this.duration) * 100;
      }
      
      // 发送进度变化事件
      this.$emit('progress-change', this.currentTime);
    },
    
    // 视频加载完成处理
    onVideoLoaded() {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      this.duration = video.duration;
    },
    
    // 视频播放处理
    onVideoPlay() {
      this.isPlaying = true;
    },
    
    // 视频暂停处理
    onVideoPause() {
      this.isPlaying = false;
    },
    
    // 视频结束处理
    onVideoEnded() {
      this.isPlaying = false;
      this.$emit('video-complete');
    },
    
    // 设置播放速度
    setPlaybackRate(rate) {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      this.playbackRate = rate;
      video.playbackRate = rate;
      this.showPlaybackRateMenu = false;
    },
    
    // 切换播放速度菜单
    togglePlaybackRateMenu() {
      this.showPlaybackRateMenu = !this.showPlaybackRateMenu;
    },
    
    // 关闭播放速度菜单
    closePlaybackRateMenu() {
      this.showPlaybackRateMenu = false;
    },
    
    // 跳转到指定位置
    seekVideo(event) {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      const progressBar = event.currentTarget;
      const clickPosition = (event.clientX - progressBar.getBoundingClientRect().left) / progressBar.offsetWidth;
      
      video.currentTime = clickPosition * video.duration;
    },
    
    // 键盘事件处理
    handleKeyDown(e) {
      const video = this.$refs.videoElement;
      if (!video) return;
      
      switch (e.key) {
        case ' ':
          // 空格键：播放/暂停
          this.togglePlay();
          e.preventDefault();
          break;
        case 'ArrowRight':
          // 右箭头：快进10秒
          video.currentTime += 10;
          e.preventDefault();
          break;
        case 'ArrowLeft':
          // 左箭头：后退10秒
          video.currentTime -= 10;
          e.preventDefault();
          break;
        case 'ArrowUp':
          // 上箭头：增加音量
          this.volume = Math.min(this.volume + 0.1, 1);
          video.volume = this.volume;
          e.preventDefault();
          break;
        case 'ArrowDown':
          // 下箭头：减少音量
          this.volume = Math.max(this.volume - 0.1, 0);
          video.volume = this.volume;
          e.preventDefault();
          break;
        case 'f':
          // f键：全屏
          this.toggleFullscreen();
          e.preventDefault();
          break;
        case 'm':
          // m键：静音
          this.toggleMute();
          e.preventDefault();
          break;
      }
    }
  }
}
</script>

<style scoped>
.video-player {
  position: relative;
  width: 100%;
  background-color: #000;
  overflow: hidden;
}

.video-element {
  width: 100%;
  height: auto;
  display: block;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  padding: 16px;
  transition: opacity 0.3s;
}

.progress-container {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.progress-bar {
  flex-grow: 1;
  height: 6px;
  background-color: rgba(255, 255, 255, 0.3);
  border-radius: 9999px;
  margin-right: 10px;
  cursor: pointer;
  position: relative;
}

.progress-filled {
  height: 100%;
  background-color: #2563EB;
  border-radius: 9999px;
}

.time-display {
  color: #fff;
  font-size: 20px;
  min-width: 100px;
  text-align: right;
}

.control-buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.control-button {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.playback-rate {
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  position: relative;
  padding: 0 10px;
}

.playback-rate-menu {
  position: absolute;
  bottom: 30px;
  left: 0;
  background-color: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
  padding: 5px 0;
  z-index: 10;
}

.playback-rate-item {
  padding: 5px 15px;
  white-space: nowrap;
}

.playback-rate-item:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.playback-rate-item.active {
  color: #2563EB;
}

.volume-control {
  display: flex;
  align-items: center;
}

.volume-slider-container {
  width: 100px;
  margin: 0 10px;
}

.volume-slider {
  width: 100%;
  -webkit-appearance: none;
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 2px;
  outline: none;
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
}

.volume-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  border: none;
}

/* 全屏样式 */
.video-player:fullscreen {
  width: 100vw;
  height: 100vh;
}

.video-player:fullscreen .video-element {
  width: 100%;
  height: 100%;
}
</style>
