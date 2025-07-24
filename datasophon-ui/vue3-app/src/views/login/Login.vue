<template>
  <div 
      class="login-container" 
      @mousemove="handleGlobalMouseMove" 
      @click="handleGlobalClick"
      ref="containerRef"
    >

    <!-- 动态背景 -->
    <div class="background">
      <!-- 多层次动态背景 -->
      <div class="blur-circle circle-1"></div>
      <div class="blur-circle circle-2"></div>
      <div class="blur-circle circle-3"></div>
      <div class="blur-circle circle-4"></div>
      <div class="blur-circle circle-5"></div>
      
      <!-- 增强星星点点效果 -->
      <div class="stars">
        <div v-for="n in 100" :key="`star-${n}`" class="star" :style="`--index: ${n}`"></div>
      </div>
      
      <!-- 流星效果 -->
      <div class="meteors">
        <div v-for="n in 5" :key="`meteor-${n}`" class="meteor" :style="`--delay: ${n * 3}s`"></div>
      </div>
      
      <!-- 网格背景 -->
      <div class="grid-overlay"></div>
      
      <!-- 增强动态粒子 -->
      <div class="particles">
        <div v-for="n in 50" :key="`particle-${n}`" class="particle" :style="`--i: ${n}`"></div>
      </div>
      
      <!-- 浮动几何图形 -->
      <div class="floating-shapes">
        <div v-for="n in 8" :key="`shape-${n}`" class="floating-shape" :style="`--index: ${n}`"></div>
      </div>
      
      <!-- 光影效果 -->
      <div class="shimmer-effect"></div>
      
      <!-- 增强渐变光线 -->
      <div class="light-beam light-beam-1"></div>
      <div class="light-beam light-beam-2"></div>
      <div class="light-beam light-beam-3"></div>
      
      <!-- 脉冲波纹 -->
      <div class="pulse-waves">
        <div v-for="n in 3" :key="`wave-${n}`" class="pulse-wave" :style="`--delay: ${n * 2}s`"></div>
      </div>
      
      <!-- 动态渐变覆盖 -->
      <div class="dynamic-gradient"></div>
      
      <!-- 边缘光效 -->
      <div class="edge-glow edge-glow-top"></div>
      <div class="edge-glow edge-glow-bottom"></div>
      <div class="edge-glow edge-glow-left"></div>
      <div class="edge-glow edge-glow-right"></div>
    </div>
    
    <!-- 登录卡片 -->
    <div 
      class="login-card"
      :class="{'active': cardActive}"
      ref="loginCard"
      @mousemove="handleCardMouseMove"
      @mouseleave="handleCardMouseLeave"
    >
      <!-- 卡片内部光晕 -->
      <div class="inner-glow"></div>
      
      <!-- 品牌区域 -->
      <div class="brand-section">
        <div class="logo-container" @mouseenter="logoHover = true" @mouseleave="logoHover = false">
          <div class="logo-orbit" :class="{'animate': logoHover}">
            <span v-for="n in 4" :key="`orbit-${n}`" class="orbit-dot"></span>
          </div>
          <img src="@/assets/img/logo.png" alt="Datasophon Logo" class="logo" />
        </div>
        <h1 class="brand-title">Datasophon</h1>
        <p class="brand-subtitle">一站式大数据平台部署与管理系统</p>
      </div>
      
      <!-- 表单区域 -->
      <div class="form-section">
        <form @submit.prevent="handleLogin">
          <!-- 用户名输入框 -->
          <div class="input-group" :class="{ 'focus': activeField === 'username', 'error': v$.username.$error }">
            <label for="username">用户名</label>
            <div class="input-wrapper">
              <input
                id="username"
                type="text"
                v-model="loginForm.username"
                @focus="setActiveField('username')"
                @blur="handleFieldBlur('username')"
                placeholder="请输入用户名"
                autocomplete="username"
              />
              <span class="input-focus-effect"></span>
            </div>
            <transition name="fade">
              <p v-if="v$.username.$error" class="error-text">{{ v$.username.$errors[0].$message }}</p>
            </transition>
          </div>
          
          <!-- 密码输入框 -->
          <div class="input-group" :class="{ 'focus': activeField === 'password', 'error': v$.password.$error }">
            <label for="password">密码</label>
            <div class="input-wrapper">
              <input
                id="password"
                :type="passwordVisible ? 'text' : 'password'"
                v-model="loginForm.password"
                @focus="setActiveField('password')"
                @blur="handleFieldBlur('password')"
                placeholder="请输入密码"
                autocomplete="current-password"
              />
              <button 
                type="button" 
                class="visibility-toggle"
                @click="togglePasswordVisibility"
                @mouseenter="eyeHover = true"
                @mouseleave="eyeHover = false"
              >
                <svg class="eye-icon" :class="{'visible': passwordVisible, 'hover': eyeHover}" viewBox="0 0 24 24" fill="none">
                  <path v-if="!passwordVisible" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle v-if="!passwordVisible" cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path v-if="passwordVisible" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <line v-if="passwordVisible" x1="1" y1="1" x2="23" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <span class="input-focus-effect"></span>
            </div>
            <transition name="fade">
              <p v-if="v$.password.$error" class="error-text">{{ v$.password.$errors[0].$message }}</p>
            </transition>
          </div>
          
          <!-- 记住我选项 -->
          <div class="form-options">
            <label class="remember-option">
              <input
                type="checkbox"
                v-model="loginForm.rememberMe"
                id="remember-me"
              />
              <span class="checkmark">
                <svg viewBox="0 0 24 24" class="checkmark-svg">
                  <path class="checkmark-path" d="M5 13l4 4L19 7"></path>
                </svg>
              </span>
              <span>记住我</span>
            </label>
          </div>
          
          <!-- 登录按钮 -->
          <div class="button-wrapper">
            <div class="button-shadow"></div>
          <button
            type="submit"
              class="login-button"
              :disabled="userStore.loading || !isFormValid"
              :class="{'loading': userStore.loading}"
              @mouseenter="buttonHover = true"
              @mouseleave="buttonHover = false"
            >
              <span class="button-bg"></span>
              <span class="button-highlight" :class="{'hover': buttonHover}"></span>
              <span class="button-text">{{ userStore.loading ? '登录中...' : '登录' }}</span>
              <div class="loading-dots" v-if="userStore.loading">
                <span></span><span></span><span></span>
              </div>
          </button>
          </div>
          
          <!-- 错误消息 -->
          <transition name="fade">
            <div v-if="errorMsg" class="error-message">
              <div class="error-icon">
                <svg viewBox="0 0 24 24">
                  <path d="M12 8v5M12 16h.01M22 12c0 5.523-4.477 10-10 10S2 17.523 2 12 6.477 2 12 2s10 4.477 10 10z"></path>
                </svg>
              </div>
              <p>{{ errorMsg }}</p>
              <button type="button" class="close-button" @click="dismissError">
                <svg viewBox="0 0 24 24">
                  <path d="M18 6L6 18M6 6l12 12"></path>
                </svg>
              </button>
          </div>
          </transition>
        </form>
      </div>
      
      <!-- 页脚信息 -->
      <div class="footer">
        <div class="feature-tags">
          <div class="feature-tag">
            <span class="tag-dot"></span>
            <span class="tag-text">智能管理</span>
          </div>
          <div class="feature-tag">
            <span class="tag-dot"></span>
            <span class="tag-text">高可用性</span>
          </div>
          <div class="feature-tag">
            <span class="tag-dot"></span>
            <span class="tag-text">多用户支持</span>
          </div>
        </div>
        <p class="copyright">© {{ currentYear }} Datasophon</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';
import { required, minLength } from '@vuelidate/validators';
import { useUserStore } from '@/stores/user';

// 路由和状态
const router = useRouter();
const userStore = useUserStore();
const containerRef = ref(null);

// 表单数据
const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
  rememberMe: false
});

// UI 状态
const activeField = ref(null);
const passwordVisible = ref(false);
const errorMsg = ref('');
const cardActive = ref(false);
const loginCard = ref(null);
const logoHover = ref(false);
const eyeHover = ref(false);
const buttonHover = ref(false);

// 鼠标位置
const mousePosition = reactive({ x: 0, y: 0 });





// 表单验证规则
const rules = {
  username: { 
    required: v => !!v || '请输入用户名' 
  },
  password: { 
    required: v => !!v || '请输入密码',
    minLength: v => !v || v.length >= 6 || '密码至少需要6个字符'
  }
};

// 验证实例
const v$ = useVuelidate(rules, loginForm);

// 计算属性
const isFormValid = computed(() => !v$.value.$invalid);
const currentYear = computed(() => new Date().getFullYear());

// 设置当前激活的输入字段
const setActiveField = (field) => {
  activeField.value = field;
  cardActive.value = true;
};

// 处理字段失焦
const handleFieldBlur = async (field) => {
  activeField.value = null;
  await v$.value[field].$touch();
};

// 切换显示/隐藏密码
const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
};



// 关闭错误消息
const dismissError = () => {
  errorMsg.value = '';
};

// 登录处理
const handleLogin = async () => {
  const isFormCorrect = await v$.value.$validate();
  if (!isFormCorrect) return;
  
  errorMsg.value = '';
    
  try {
    const userData = await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    });
    
    router.push('/');
  } catch (error) {
    errorMsg.value = error.message || '登录失败，请检查用户名和密码';
    
    // 添加卡片震动效果
    if (loginCard.value) {
      loginCard.value.classList.add('shake');
      setTimeout(() => loginCard.value.classList.remove('shake'), 820);
    }
  }
};

// 处理全局鼠标移动
const handleGlobalMouseMove = (event) => {
  // 更新动态背景元素位置
  updateDynamicElements(event);
};

// 更新动态元素
const updateDynamicElements = (event) => {
  const { clientX, clientY } = event;
  const centerX = window.innerWidth / 2;
  const centerY = window.innerHeight / 2;
  
  const offsetX = (clientX - centerX) / centerX;
  const offsetY = (clientY - centerY) / centerY;
  
  // 更新模糊圆圈位置
  const circles = document.querySelectorAll('.blur-circle');
  circles.forEach((circle, index) => {
    const factor = (index + 1) * 0.1;
    circle.style.transform = `translate(${offsetX * 50 * factor}px, ${offsetY * 50 * factor}px)`;
  });
  
  // 更新浮动图形
  const shapes = document.querySelectorAll('.floating-shape');
  shapes.forEach((shape, index) => {
    const factor = (index + 1) * 0.05;
    shape.style.transform = `translate(${offsetX * 30 * factor}px, ${offsetY * 30 * factor}px) rotate(${offsetX * 10}deg)`;
  });
};

// 处理全局点击
const handleGlobalClick = (event) => {
  createClickRipple(event.clientX, event.clientY);
  playClickSound();
};

// 创建点击波纹效果
const createClickRipple = (x, y) => {
  const ripple = document.createElement('div');
  ripple.className = 'click-ripple';
  ripple.style.left = (x - 25) + 'px';
  ripple.style.top = (y - 25) + 'px';
  
  document.body.appendChild(ripple);
  
  setTimeout(() => {
    ripple.remove();
  }, 1000);
};

// 播放点击音效（可选）
const playClickSound = () => {
  // 创建音频上下文进行音效播放
  try {
    const audioContext = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioContext.createOscillator();
    const gainNode = audioContext.createGain();
    
    oscillator.connect(gainNode);
    gainNode.connect(audioContext.destination);
    
    oscillator.frequency.setValueAtTime(800, audioContext.currentTime);
    oscillator.frequency.exponentialRampToValueAtTime(400, audioContext.currentTime + 0.1);
    
    gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
    gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1);
    
    oscillator.start(audioContext.currentTime);
    oscillator.stop(audioContext.currentTime + 0.1);
  } catch (e) {
    // 静默处理音频错误
  }
};

// 鼠标移动效果
const handleCardMouseMove = (e) => {
  if (!loginCard.value) return;
  
  const card = loginCard.value;
  const rect = card.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  
  mousePosition.x = x / rect.width;
  mousePosition.y = y / rect.height;
  
  const centerX = rect.width / 2;
  const centerY = rect.height / 2;
  const rotateY = (x - centerX) / 20;
  const rotateX = (centerY - y) / 20;
  
  card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.02)`;
};

// 鼠标离开时重置卡片效果
const handleCardMouseLeave = () => {
  if (!loginCard.value) return;
  
  loginCard.value.style.transform = 'perspective(1000px) rotateX(0) rotateY(0) scale(1)';
  
  // 如果没有聚焦任何字段，重置卡片激活状态
  if (!activeField.value) {
    cardActive.value = false;
  }
};

// 初始化增强特效
const initEnhancedEffects = () => {
  // 初始化星星动画
  const stars = document.querySelectorAll('.star');
  stars.forEach((star, index) => {
    const delay = Math.random() * 5;
    const duration = 2 + Math.random() * 3;
    star.style.animationDelay = `${delay}s`;
    star.style.animationDuration = `${duration}s`;
  });
  
  // 初始化浮动图形
  const shapes = document.querySelectorAll('.floating-shape');
  shapes.forEach((shape, index) => {
    const delay = Math.random() * 3;
    const duration = 8 + Math.random() * 4;
    shape.style.animationDelay = `${delay}s`;
    shape.style.animationDuration = `${duration}s`;
  });
  
  // 启动背景动画循环
  startBackgroundAnimations();
};

// 启动背景动画循环
const startBackgroundAnimations = () => {
  // 动态渐变动画
  const gradient = document.querySelector('.dynamic-gradient');
  if (gradient) {
    setInterval(() => {
      const hue = Math.random() * 360;
      gradient.style.background = `
        radial-gradient(circle at ${Math.random() * 100}% ${Math.random() * 100}%, 
          hsla(${hue}, 70%, 50%, 0.1) 0%, 
          transparent 50%),
        radial-gradient(circle at ${Math.random() * 100}% ${Math.random() * 100}%, 
          hsla(${(hue + 120) % 360}, 70%, 50%, 0.1) 0%, 
          transparent 50%)
      `;
    }, 3000);
  }
};

// 初始化
onMounted(() => {
  // 激活进入动画
  setTimeout(() => {
    cardActive.value = true;
    // 2秒后重置卡片状态，除非用户正在交互
    setTimeout(() => {
      if (!activeField.value) {
        cardActive.value = false;
      }
    }, 2000);
  }, 500);
  
  // 添加背景粒子动画
  document.querySelectorAll('.particle').forEach(particle => {
    const delay = Math.random() * 4;
    const duration = 3 + Math.random() * 5;
    
    particle.style.setProperty('--delay', `${delay}s`);
    particle.style.setProperty('--duration', `${duration}s`);
  });
  
  // 初始化增强特效
  initEnhancedEffects();
});
</script>

<style scoped>
/* 全局容器 */
.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
  color: #fff;
  padding: 20px;
  position: relative;
  overflow: hidden;
  background-color: #000511;

}



/* 背景效果 */
.background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: linear-gradient(125deg, #000511 0%, #001041 50%, #000e2c 100%);
}

/* 模糊圆圈 */
.blur-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.8;
  mix-blend-mode: screen;
}

.circle-1 {
  width: 500px;
  height: 500px;
  background: rgba(59, 130, 246, 0.15);
  top: -100px;
  right: -100px;
  animation: float 20s ease-in-out infinite alternate;
}

.circle-2 {
  width: 600px;
  height: 600px;
  background: rgba(99, 102, 241, 0.1);
  bottom: -200px;
  left: -200px;
  animation: float 25s ease-in-out infinite alternate-reverse;
}

.circle-3 {
  width: 300px;
  height: 300px;
  background: rgba(168, 85, 247, 0.12);
  top: 40%;
  left: 30%;
  animation: float 18s ease-in-out 2s infinite alternate;
}

.circle-4 {
  width: 180px;
  height: 180px;
  background: rgba(34, 197, 94, 0.1);
  top: 30%;
  right: 30%;
  animation: float 15s ease-in-out 1s infinite alternate;
}

.circle-5 {
  width: 220px;
  height: 220px;
  background: rgba(251, 146, 60, 0.1);
  bottom: 40%;
  right: 20%;
  animation: float 22s ease-in-out 3s infinite alternate;
}



/* 点击波纹效果 */
.click-ripple {
  position: fixed;
  width: 50px;
  height: 50px;
  border: 2px solid rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  pointer-events: none;
  z-index: 9997;
  animation: rippleExpand 1s ease-out forwards;
}

@keyframes rippleExpand {
  0% {
    transform: scale(0);
    opacity: 1;
  }
  100% {
    transform: scale(4);
    opacity: 0;
  }
}

/* 流星效果 */
.meteors {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.meteor {
  position: absolute;
  width: 2px;
  height: 2px;
  background: linear-gradient(45deg, rgba(255, 255, 255, 1) 0%, transparent 100%);
  border-radius: 50%;
  top: -5%;
  left: calc(10% + var(--delay, 0s) * 15%);
  animation: meteorFall 8s linear var(--delay, 0s) infinite;
}

@keyframes meteorFall {
  0% {
    transform: translateY(-100vh) translateX(0) scale(0);
    opacity: 0;
  }
  10% {
    transform: translateY(-80vh) translateX(20px) scale(1);
    opacity: 1;
    box-shadow: 0 0 10px rgba(255, 255, 255, 0.8), 0 0 20px rgba(255, 255, 255, 0.4);
  }
  90% {
    transform: translateY(100vh) translateX(200px) scale(1);
    opacity: 1;
    box-shadow: 0 0 10px rgba(255, 255, 255, 0.8), 0 0 20px rgba(255, 255, 255, 0.4);
  }
  100% {
    transform: translateY(120vh) translateX(220px) scale(0);
    opacity: 0;
  }
}

/* 浮动几何图形 */
.floating-shapes {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.floating-shape {
  position: absolute;
  width: 20px;
  height: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  top: calc(10% + (var(--index) % 8) * 10%);
  left: calc(10% + (var(--index) % 10) * 8%);
  animation: shapeFloat 8s ease-in-out infinite;
  transition: transform 0.3s ease;
}

.floating-shape:nth-child(odd) {
  border-radius: 50%;
  background: rgba(59, 130, 246, 0.1);
}

.floating-shape:nth-child(even) {
  transform: rotate(45deg);
  background: rgba(168, 85, 247, 0.1);
}

@keyframes shapeFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  25% {
    transform: translateY(-20px) rotate(90deg);
  }
  50% {
    transform: translateY(-10px) rotate(180deg);
  }
  75% {
    transform: translateY(-30px) rotate(270deg);
  }
}

/* 脉冲波纹 */
.pulse-waves {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.pulse-wave {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: pulseExpand 6s ease-out var(--delay, 0s) infinite;
}

@keyframes pulseExpand {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(8);
    opacity: 0;
  }
}

/* 动态渐变覆盖 */
.dynamic-gradient {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: radial-gradient(circle at 50% 50%, 
    hsla(240, 70%, 50%, 0.1) 0%, 
    transparent 50%);
  animation: gradientShift 10s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% {
    background: radial-gradient(circle at 20% 30%, 
      hsla(240, 70%, 50%, 0.1) 0%, 
      transparent 50%);
  }
  25% {
    background: radial-gradient(circle at 80% 20%, 
      hsla(300, 70%, 50%, 0.1) 0%, 
      transparent 50%);
  }
  50% {
    background: radial-gradient(circle at 70% 80%, 
      hsla(180, 70%, 50%, 0.1) 0%, 
      transparent 50%);
  }
  75% {
    background: radial-gradient(circle at 30% 70%, 
      hsla(60, 70%, 50%, 0.1) 0%, 
      transparent 50%);
  }
}

/* 边缘光效 */
.edge-glow {
  position: absolute;
  z-index: 1;
}

.edge-glow-top {
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(59, 130, 246, 0.5) 50%, 
    transparent 100%);
  animation: edgeGlowMove 8s ease-in-out infinite;
}

.edge-glow-bottom {
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(168, 85, 247, 0.5) 50%, 
    transparent 100%);
  animation: edgeGlowMove 8s ease-in-out 2s infinite;
}

.edge-glow-left {
  top: 0;
  bottom: 0;
  left: 0;
  width: 2px;
  background: linear-gradient(0deg, 
    transparent 0%, 
    rgba(34, 197, 94, 0.5) 50%, 
    transparent 100%);
  animation: edgeGlowMove 8s ease-in-out 4s infinite;
}

.edge-glow-right {
  top: 0;
  bottom: 0;
  right: 0;
  width: 2px;
  background: linear-gradient(0deg, 
    transparent 0%, 
    rgba(251, 146, 60, 0.5) 50%, 
    transparent 100%);
  animation: edgeGlowMove 8s ease-in-out 6s infinite;
}

@keyframes edgeGlowMove {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

@keyframes float {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, 20px) scale(1.05);
  }
  100% {
    transform: translate(50px, 30px) scale(1);
  }
}

/* 星星效果 */
.stars {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.star {
  position: absolute;
  width: 2px;
  height: 2px;
  background: white;
  border-radius: 50%;
  opacity: calc(0.2 + (var(--index) % 10) * 0.05);
  top: calc((var(--index) % 10) * 10%);
  left: calc((var(--index) % 15) * 7%);
  animation: twinkle calc(2s + (var(--index) % 4)) ease-in-out infinite alternate;
}

@keyframes twinkle {
  0%, 100% {
    opacity: calc(0.2 + (var(--index) % 10) * 0.05);
    transform: scale(1);
  }
  50% {
    opacity: calc(0.1 + (var(--index) % 10) * 0.05);
    transform: scale(0.5);
  }
}

/* 网格背景 */
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(99, 102, 241, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.5;
  perspective: 500px;
  transform-style: preserve-3d;
  transform: rotateX(60deg) translateZ(-100px);
  animation: grid-pulse 20s linear infinite;
}

@keyframes grid-pulse {
  0% {
    background-size: 40px 40px;
    opacity: 0.5;
  }
  50% {
    background-size: 42px 42px;
    opacity: 0.6;
  }
  100% {
    background-size: 40px 40px;
    opacity: 0.5;
  }
}

/* 动态粒子 */
.particles {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  filter: blur(1px);
  top: calc(10% + (var(--i) % 8) * 10%);
  left: calc(10% + (var(--i) % 10) * 8%);
  animation: particle-float var(--duration, 5s) ease-in-out var(--delay, 0s) infinite alternate;
}

@keyframes particle-float {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.3;
    background: rgba(59, 130, 246, 0.3);
  }
  100% {
    transform: translate(20px, 20px) scale(1.5);
    opacity: 0.1;
    background: rgba(168, 85, 247, 0.3);
  }
}

/* 闪烁效果 */
.shimmer-effect {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.05),
    transparent
  );
  background-size: 200% 100%;
  animation: shimmer 10s infinite;
}

@keyframes shimmer {
  0% {
    background-position: -100% 0;
  }
  100% {
    background-position: 300% 0;
  }
}

/* 光线效果 */
.light-beam {
  position: absolute;
  height: 1px;
  width: 100%;
  background: linear-gradient(90deg, transparent, rgba(99, 102, 241, 0.2), transparent);
  opacity: 0.5;
  transform-origin: left;
}

.light-beam-1 {
  top: 30%;
  animation: light-move 15s linear infinite;
}

.light-beam-2 {
  top: 70%;
  animation: light-move 18s linear 2s infinite;
}

.light-beam-3 {
  top: 50%;
  animation: light-move 12s linear 4s infinite;
  background: linear-gradient(90deg, transparent, rgba(168, 85, 247, 0.2), transparent);
}

@keyframes light-move {
  0% {
    transform: translateX(-100%) scaleY(1);
    opacity: 0;
  }
  20% {
    transform: translateX(-20%) scaleY(1);
    opacity: 0.5;
  }
  80% {
    transform: translateX(20%) scaleY(1);
    opacity: 0.5;
  }
  100% {
    transform: translateX(100%) scaleY(1);
    opacity: 0;
  }
}

/* 登录卡片 */
.login-card {
  width: 380px;
  max-width: 95%;
  background: rgba(20, 30, 65, 0.3);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow: 
    0 4px 48px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
  padding: 40px 32px;
  transition: 
    transform 0.5s cubic-bezier(0.13, 0.53, 0.38, 0.97),
    box-shadow 0.5s cubic-bezier(0.13, 0.53, 0.38, 0.97);
  transform-style: preserve-3d;
  position: relative;
  z-index: 10;
  overflow: hidden;
  animation: card-appear 1s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
  transform: translateY(20px);
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0) 100%);
  border-radius: 20px;
  z-index: -1;
}

.inner-glow {
  position: absolute;
  inset: 0;
  border-radius: 20px;
  background: linear-gradient(to right, transparent, rgba(99, 102, 241, 0.05), transparent);
  opacity: 0;
  transition: opacity 0.5s ease;
}

.login-card.active .inner-glow {
  opacity: 1;
  animation: inner-glow 5s ease-in-out infinite alternate;
}

@keyframes inner-glow {
  0% {
    background-position: -100% 0;
    opacity: 0.2;
  }
  100% {
    background-position: 200% 0;
    opacity: 0.5;
  }
}

.login-card.active {
  box-shadow: 
    0 8px 60px rgba(0, 0, 0, 0.3),
    0 0 120px rgba(99, 102, 241, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.12) inset;
}

@keyframes card-appear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 卡片震动 */
@keyframes shake {
  10%, 90% { transform: perspective(1000px) translate3d(-1px, 0, 0); }
  20%, 80% { transform: perspective(1000px) translate3d(2px, 0, 0); }
  30%, 50%, 70% { transform: perspective(1000px) translate3d(-4px, 0, 0); }
  40%, 60% { transform: perspective(1000px) translate3d(4px, 0, 0); }
}

.shake {
  animation: shake 0.82s cubic-bezier(.36,.07,.19,.97) both;
}

/* 品牌区域 */
.brand-section {
  text-align: center;
  margin-bottom: 36px;
}

.logo-container {
  margin: 0 auto 16px;
  width: 72px;
  height: 72px;
  position: relative;
  cursor: pointer;
}

.logo-orbit {
  position: absolute;
  inset: -10px;
  border: 1px solid rgba(99, 102, 241, 0.1);
  border-radius: 50%;
  transition: all 0.5s ease;
}

.logo-orbit.animate {
  border-color: rgba(99, 102, 241, 0.3);
  animation: orbit-rotate 8s linear infinite;
}

.orbit-dot {
  position: absolute;
  width: 6px;
  height: 6px;
  background: rgba(99, 102, 241, 0.5);
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(calc(var(--n) * 90deg)) translateX(46px);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.logo-orbit.animate .orbit-dot {
  opacity: 1;
}

@keyframes orbit-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  position: relative;
  animation: logo-pulse 5s ease-in-out infinite alternate;
  filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.3));
  transition: transform 0.3s ease, filter 0.3s ease;
}

.logo-container:hover .logo {
  transform: scale(1.05);
  filter: drop-shadow(0 0 12px rgba(99, 102, 241, 0.5));
}

@keyframes logo-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.03);
  }
}

.brand-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px;
  background: linear-gradient(to right, #fff, #a5b4fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: title-appear 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards;
  opacity: 0;
  transform: translateY(10px);
  letter-spacing: -0.5px;
}

.brand-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  animation: title-appear 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.3s forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes title-appear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 表单区域 */
.form-section {
  margin-bottom: 20px;
}

.form-section form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 输入组 */
.input-group {
  position: relative;
  animation: fade-in 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  opacity: 0;
}

.input-group:nth-child(1) {
  animation-delay: 0.4s;
}

.input-group:nth-child(2) {
  animation-delay: 0.5s;
}

.input-group:nth-child(3) {
  animation-delay: 0.6s;
}

@keyframes fade-in {
  to {
    opacity: 1;
  }
}

.input-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.8);
  transition: color 0.2s;
}

.input-group.focus label {
  color: #a5b4fc;
}

.input-wrapper {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
}

.input-group input {
  width: 100%;
  height: 46px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 0 16px;
  color: #fff;
  font-size: 15px;
  transition: all 0.25s;
  outline: none;
  position: relative;
  z-index: 1;
}

.input-focus-effect {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(to right, #4f46e5, #a5b4fc);
  transition: width 0.3s ease, left 0.3s ease;
}

.input-group.focus .input-focus-effect {
  width: 100%;
  left: 0;
}

.input-group.focus input {
  background: rgba(255, 255, 255, 0.09);
  border-color: rgba(165, 180, 252, 0.5);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
}

.input-group input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.password-input {
  position: relative;
}

.visibility-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: transparent;
  border: none;
  padding: 6px;
  cursor: pointer;
  z-index: 2;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: rgba(255, 255, 255, 0.6);
}

.visibility-toggle:hover {
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.9);
  transform: translateY(-50%) scale(1.05);
}

.visibility-toggle:active {
  transform: translateY(-50%) scale(0.95);
  transition: transform 0.1s ease;
}

/* SVG眼睛图标 */
.eye-icon {
  width: 20px;
  height: 20px;
  transition: all 0.2s ease;
  stroke: currentColor;
}

.eye-icon.hover {
  color: rgba(99, 102, 241, 0.8);
  transform: scale(1.1);
}

.eye-icon.visible {
  color: rgba(99, 102, 241, 0.7);
}

.error-text {
  color: #f87171;
  font-size: 12px;
  margin-top: 6px;
}

.input-group.error input {
  border-color: rgba(248, 113, 113, 0.5);
  box-shadow: 0 0 0 4px rgba(248, 113, 113, 0.15);
}

/* 记住我选项 */
.form-options {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 8px;
}

.remember-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  position: relative;
}

.remember-option input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: relative;
  display: inline-block;
  width: 18px;
  height: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  transition: all 0.2s;
  overflow: hidden;
}

.remember-option:hover .checkmark {
  background: rgba(255, 255, 255, 0.15);
}

.remember-option input:checked ~ .checkmark {
  background: #6366f1;
  border-color: #6366f1;
}

.checkmark-svg {
  position: absolute;
  inset: 0;
  stroke: white;
  stroke-width: 2;
  stroke-dasharray: 22;
  stroke-dashoffset: 66;
  stroke-linecap: round;
  stroke-linejoin: round;
  fill: none;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  transform: scale(0.7);
  opacity: 0;
}

.remember-option input:checked ~ .checkmark .checkmark-svg {
  stroke-dashoffset: 44;
  opacity: 1;
}



/* 登录按钮 */
.button-wrapper {
  position: relative;
  margin-top: 10px;
}

.button-shadow {
  position: absolute;
  inset: 4px 0 0 0;
  background: #4f46e5;
  filter: blur(12px);
  opacity: 0.3;
  border-radius: 12px;
  transition: all 0.3s ease;
  z-index: 0;
}

.login-button {
  position: relative;
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
  overflow: hidden;
  z-index: 1;
}

.button-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  transition: all 0.3s ease;
  z-index: -1;
}

.button-highlight {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    to bottom,
    rgba(255, 255, 255, 0.1) 0%,
    transparent 50%
  );
  opacity: 0.5;
  transition: opacity 0.3s ease;
  z-index: 0;
}

.button-highlight.hover {
  opacity: 0.8;
}

.login-button:hover {
  transform: translateY(-2px);
}

.login-button:hover + .button-shadow {
  opacity: 0.4;
  filter: blur(16px);
}

.login-button:active {
  transform: translateY(1px);
}

.login-button:disabled {
  background: linear-gradient(135deg, #9ca3af, #6b7280);
  cursor: not-allowed;
  transform: none;
}

.button-text {
  position: relative;
  z-index: 1;
  transition: opacity 0.2s;
}

.loading-dots {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: dots 1.4s infinite ease-in-out;
}

.loading-dots span:nth-child(1) {
  animation-delay: 0s;
}

.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}

.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dots {
  0%, 100% {
    transform: scale(0.6);
    opacity: 0.6;
  }
  50% {
    transform: scale(1);
    opacity: 1;
  }
}

.login-button.loading .button-text {
  opacity: 0;
}

/* 错误消息 */
.error-message {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-top: 12px;
  background: rgba(248, 113, 113, 0.1);
  border: 1px solid rgba(248, 113, 113, 0.2);
  border-radius: 12px;
  position: relative;
  overflow: hidden;
}

.error-message::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, transparent, rgba(248, 113, 113, 0.1), transparent);
  transform: translateX(-100%);
  animation: error-shine 2s infinite;
}

@keyframes error-shine {
  0% {
    transform: translateX(-100%);
  }
  50%, 100% {
    transform: translateX(100%);
  }
}

.error-icon {
  margin-right: 10px;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
}

.error-icon svg {
  width: 100%;
  height: 100%;
  stroke: #f87171;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.error-message p {
  color: #f87171;
  font-size: 14px;
  margin: 0;
  flex-grow: 1;
}

.close-button {
  background: transparent;
  border: none;
  width: 20px;
  height: 20px;
  padding: 0;
  margin-left: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.close-button svg {
  width: 16px;
  height: 16px;
  stroke: #f87171;
  fill: none;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.close-button:hover {
  background-color: rgba(248, 113, 113, 0.15);
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 特性标签区域 */
.feature-tags {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 16px;
  animation: feature-appear 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.7s forwards;
  opacity: 0;
  transform: translateY(10px);
}

@keyframes feature-appear {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.feature-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  transition: all 0.3s ease;
}

.feature-tag:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}

.tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a5b4fc;
}

.tag-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

/* 页脚 */
.footer {
  margin-top: 30px;
  text-align: center;
}

.copyright {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
  animation: feature-appear 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.8s forwards;
  opacity: 0;
  transform: translateY(10px);
}

/* 响应式调整 */
@media (max-width: 480px) {
  .login-card {
    padding: 32px 20px;
  }
  
  .feature-tags {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style> 

<!-- 引入Font Awesome图标库 -->
<script>
// 动态加载FontAwesome
document.addEventListener('DOMContentLoaded', function() {
  const link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css';
  document.head.appendChild(link);
});
</script>