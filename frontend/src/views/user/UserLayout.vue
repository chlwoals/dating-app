<template>
  <section class="user-app-shell">
    <div v-if="checking" class="user-app-loading">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>사용자 화면을 준비하고 있어요</h2>
        <p>로그인 상태와 계정 상태를 확인하는 중입니다.</p>
      </div>
    </div>

    <template v-else>
      <main class="user-app-main">
        <router-view />
      </main>

      <nav class="bottom-nav" aria-label="사용자 하단 내비게이션">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" class="nav-item" :class="{ active: route.path === item.to }">
          <svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path :d="item.icon" />
          </svg>
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
    icon: "M3.75 11.25 12 4.5l8.25 6.75v8.25a.75.75 0 0 1-.75.75h-5.25v-5.5h-4.5v5.5H4.5a.75.75 0 0 1-.75-.75v-8.25Z",
  },
  {
    to: "/today",
    label: "오늘의 인연",
    icon: "M12 20.25S4.5 15.88 4.5 9.9A4.1 4.1 0 0 1 8.6 5.75c1.4 0 2.63.7 3.4 1.76a4.1 4.1 0 0 1 3.4-1.76 4.1 4.1 0 0 1 4.1 4.15c0 5.98-7.5 10.35-7.5 10.35Z",
  },
  {
    to: "/chats",
    label: "채팅",
    icon: "M5.25 5.25h13.5a1.5 1.5 0 0 1 1.5 1.5v8.25a1.5 1.5 0 0 1-1.5 1.5H10.5l-4.95 3.3a.75.75 0 0 1-1.17-.62V16.5h-.63a1.5 1.5 0 0 1-1.5-1.5V6.75a1.5 1.5 0 0 1 1.5-1.5Zm2.25 4.13a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm4.5 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm4.5 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Z",
  },
  {
    to: "/profile",
    label: "프로필",
    icon: "M12 12.25a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 1.75c-4.05 0-7.25 2.28-7.25 5.08 0 .65.52 1.17 1.17 1.17h12.16c.65 0 1.17-.52 1.17-1.17 0-2.8-3.2-5.08-7.25-5.08Z",
  },
];

onMounted(async () => {
  try {
    const { data } = await api.get("/user/me");

    if (data.status === "SUSPENDED") {
      window.alert("운영 정책상 이용이 제한되었습니다.");
      clearToken();
      router.replace("/");
      return;
    }

    // 심사 전 계정도 프로필 필수 항목은 입력할 수 있어야 하므로 프로필 화면은 허용한다.
    if (data.status !== "ACTIVE" && route.path !== "/profile") {
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
.user-app-shell { min-height: 100vh; }
.user-app-main { min-height: 100vh; padding-bottom: calc(96px + env(safe-area-inset-bottom)); }
.user-app-loading { min-height: 100vh; display: grid; place-items: center; padding: 20px; }
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
.loading-card h2 { margin: 0; color: #31211d; }
.loading-card p:last-child { margin-bottom: 0; color: #6f564d; line-height: 1.6; }
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
  box-shadow: 0 18px 40px rgba(36, 20, 16, 0.28);
  z-index: 30;
}
.nav-item {
  display: grid;
  place-items: center;
  gap: 4px;
  min-height: 58px;
  padding: 8px 6px;
  border-radius: 20px;
  color: rgba(255, 245, 240, 0.7);
  text-decoration: none;
}
.nav-item.active {
  background: linear-gradient(180deg, rgba(234, 143, 100, 0.26), rgba(213, 111, 78, 0.18));
  color: #fff8f5;
}
.nav-icon {
  width: 22px;
  height: 22px;
  fill: currentColor;
  opacity: 0.9;
}
.nav-item.active .nav-icon {
  opacity: 1;
  filter: drop-shadow(0 4px 8px rgba(245, 191, 164, 0.22));
}
.nav-label { font-size: 11px; font-weight: 700; line-height: 1.1; text-align: center; }
</style>
