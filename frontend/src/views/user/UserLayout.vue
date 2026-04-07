<template>
  <section class="user-app-shell">
    <div v-if="checking" class="user-app-loading">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>앱 화면을 준비하고 있어요</h2>
        <p>로그인 상태와 계정 상태를 확인하는 중입니다.</p>
      </div>
    </div>

    <template v-else>
      <main class="user-app-main">
        <router-view />
      </main>

      <nav class="bottom-nav" aria-label="사용자 하단 내비게이션">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: route.path === item.to }"
        >
          <span class="nav-icon" v-html="item.icon" aria-hidden="true"></span>
          <span class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const route = useRoute();
const router = useRouter();
const checking = ref(true);

const navItems = [
  {
    to: "/home",
    label: "홈",
    icon:
      '<svg viewBox="0 0 24 24" fill="none"><path d="M4 10.5 12 4l8 6.5v8a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1v-8Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  },
  {
    to: "/today",
    label: "오늘의 인연",
    icon:
      '<svg viewBox="0 0 24 24" fill="none"><path d="M12 20s-6.5-3.9-8.5-8.2C1.8 8.3 3.4 5 7 5c2 0 3.4 1 5 2.8C13.6 6 15 5 17 5c3.6 0 5.2 3.3 3.5 6.8C18.5 16.1 12 20 12 20Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/></svg>',
  },
  {
    to: "/chats",
    label: "채팅",
    icon:
      '<svg viewBox="0 0 24 24" fill="none"><path d="M6 17.5h7.5l4.5 3v-3h.5a1.5 1.5 0 0 0 1.5-1.5V6A1.5 1.5 0 0 0 18.5 4.5h-13A1.5 1.5 0 0 0 4 6v10A1.5 1.5 0 0 0 5.5 17.5H6Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M8 9.5h8M8 13h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  },
  {
    to: "/profile",
    label: "프로필",
    icon:
      '<svg viewBox="0 0 24 24" fill="none"><path d="M12 12a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7ZM5 19.5c1.7-3 4.2-4.5 7-4.5s5.3 1.5 7 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>',
  },
];

onMounted(async () => {
  try {
    const { data } = await api.get("/user/me");

    if (data.status === "SUSPENDED" || data.fraudReviewStatus === "HIGH_RISK") {
      window.alert("계정 이용이 제한되었습니다. 다시 로그인해 주세요.");
      clearToken();
      router.replace("/");
      return;
    }

    if (data.status !== "ACTIVE") {
      router.replace("/review-pending");
      return;
    }
  } catch (error) {
    clearToken();
    router.replace("/");
    return;
  } finally {
    checking.value = false;
  }
});
</script>

<style scoped>
.user-app-shell {
  min-height: 100vh;
}

.user-app-main {
  min-height: 100vh;
  padding-bottom: calc(108px + env(safe-area-inset-bottom));
}

.user-app-loading {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 20px;
}

.loading-card {
  width: min(100%, 420px);
  padding: 28px 22px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.1);
  text-align: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.loading-card h2 {
  margin: 0;
  color: #31211d;
}

.loading-card p:last-child {
  margin-bottom: 0;
  color: #6f564d;
  line-height: 1.6;
}

.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: max(12px, env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: min(calc(100% - 24px), 520px);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border-radius: 28px;
  background: rgba(45, 31, 28, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 18px 40px rgba(36, 20, 16, 0.28);
  backdrop-filter: blur(18px);
  z-index: 30;
}

.nav-item {
  display: grid;
  place-items: center;
  gap: 6px;
  min-height: 64px;
  padding: 8px 6px;
  border-radius: 20px;
  color: rgba(255, 245, 240, 0.7);
  text-decoration: none;
  transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.nav-item.active {
  background: linear-gradient(180deg, rgba(234, 143, 100, 0.26), rgba(213, 111, 78, 0.18));
  color: #fff8f5;
  transform: translateY(-1px);
}

.nav-icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
}

.nav-icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.nav-label {
  font-size: 11px;
  font-weight: 700;
  line-height: 1.1;
  text-align: center;
}

@media (min-width: 768px) {
  .bottom-nav {
    width: min(calc(100% - 40px), 620px);
  }
}
</style>
