<template>
  <section class="home-page">
    <div class="home-card">
      <div class="header-row">
        <div>
          <p class="eyebrow">Welcome Back</p>
          <h1>내 프로필</h1>
        </div>
        <button class="logout-button" @click="logout">로그아웃</button>
      </div>

      <div v-if="user" class="profile-grid">
        <div class="profile-item">
          <span>이메일</span>
          <strong>{{ user.email }}</strong>
        </div>
        <div class="profile-item">
          <span>닉네임</span>
          <strong>{{ user.nickname }}</strong>
        </div>
        <div class="profile-item">
          <span>가입 방식</span>
          <strong>{{ user.provider }}</strong>
        </div>
      </div>

      <div class="policy-box">
        <strong>현재 계정 연결 상태</strong>
        <p>{{ providerMessage }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";
import { clearToken } from "../utils/auth";

const router = useRouter();
const user = ref(null);

const providerMessage = computed(() => {
  const provider = user.value?.provider;

  if (provider === "BOTH") {
    return "이메일 로그인과 Google 로그인이 모두 연결되어 있습니다.";
  }

  if (provider === "GOOGLE") {
    return "현재는 Google 로그인만 연결되어 있습니다. 같은 이메일로 회원가입하면 비밀번호 로그인도 사용할 수 있습니다.";
  }

  if (provider === "LOCAL") {
    return "현재는 이메일 로그인만 연결되어 있습니다. 같은 이메일로 Google 로그인하면 자동으로 연결됩니다.";
  }

  return "로그인 연결 상태를 확인하는 중입니다.";
});

onMounted(async () => {
  try {
    const { data } = await api.get("/user/me");
    if (data.status !== "ACTIVE") {
      router.replace("/review-pending");
      return;
    }
    user.value = data;
  } catch (error) {
    clearToken();
    router.replace("/");
  }
});

const logout = () => {
  clearToken();
  router.replace("/");
};
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding: 24px;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at top, #ffe7d6 0%, #f8c9b5 48%, #f0b39d 100%);
}

.home-card {
  width: min(100%, 720px);
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 250, 247, 0.92);
  box-shadow: 0 20px 50px rgba(109, 57, 41, 0.15);
}

.header-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: #b25736;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #37241f;
}

.logout-button {
  border: none;
  border-radius: 999px;
  padding: 12px 18px;
  background: #38201a;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.profile-grid {
  margin-top: 28px;
  display: grid;
  gap: 16px;
}

.profile-item {
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid #f0d2c6;
}

.profile-item span {
  display: block;
  margin-bottom: 8px;
  color: #936555;
  font-size: 14px;
}

.profile-item strong {
  color: #39231e;
  font-size: 18px;
}

.policy-box {
  margin-top: 22px;
  padding: 18px;
  border-radius: 18px;
  background: #fff6f1;
  border: 1px solid #efd4ca;
  color: #65463e;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #37241f;
}

.policy-box p {
  margin: 0;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .header-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>