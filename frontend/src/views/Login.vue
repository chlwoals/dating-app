<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Dating App</p>
      <h1>로그인</h1>
      <p class="description">
        이메일 계정으로 로그인해 서비스를 시작해보세요.
      </p>

      <form class="auth-form" @submit.prevent="login">
        <label>
          <span>이메일</span>
          <input v-model.trim="form.email" type="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input v-model="form.password" type="password" placeholder="8자 이상 입력" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? "로그인 중..." : "이메일로 로그인" }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="helper-row">
        <RouterLink to="/reset-password">비밀번호를 잊으셨나요?</RouterLink>
        <RouterLink to="/signup">회원가입</RouterLink>
      </div>

      <div class="policy-box">
        <strong>안내</strong>
        <p>심사 대기나 반려 상태의 계정도 로그인 후 사진 업로드 화면에서 다시 심사를 진행할 수 있습니다.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../api/api";
import { setToken } from "../utils/auth";

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const form = reactive({
  email: "",
  password: "",
});

// 로그인 후 계정 상태에 따라 홈 또는 심사 대기 화면으로 보낸다.
const login = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const { data } = await api.post("/auth/login", form);
    setToken(data.token);

    if (data.user?.status === "ACTIVE") {
      router.push("/home");
      return;
    }

    router.push("/review-pending");
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #fff3e8 0%, #ffd8c2 45%, #ffc2b3 100%);
}

.auth-card {
  width: min(100%, 440px);
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 252, 250, 0.92);
  box-shadow: 0 18px 45px rgba(98, 49, 34, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #9d4b32;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #33211d;
}

.description {
  margin: 12px 0 24px;
  color: #6e534c;
  line-height: 1.5;
}

.auth-form {
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input {
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
}

.primary-button {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  background: #d45d38;
  color: #fff;
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.helper-row {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.helper-row a {
  color: #a54123;
  font-weight: 700;
}

.policy-box {
  margin-top: 22px;
  padding: 16px;
  border-radius: 16px;
  background: #fff6f1;
  border: 1px solid #f0d0c3;
  color: #6a4a41;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3d251f;
}

.policy-box p {
  margin: 0;
  line-height: 1.5;
}
</style>