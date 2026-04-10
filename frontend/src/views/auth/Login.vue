<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Dating App</p>
      <h1>로그인</h1>
      <p class="description">이메일 계정으로 로그인해 서비스를 시작해 보세요.</p>

      <form class="auth-form" @submit.prevent="login">
        <label>
          <span>이메일</span>
          <input ref="emailInput" v-model.trim="form.email" type="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input ref="passwordInput" v-model="form.password" type="password" placeholder="8자 이상 입력" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? '로그인 중...' : '이메일로 로그인' }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="helper-row">
        <RouterLink to="/reset-password">비밀번호를 잊으셨나요?</RouterLink>
        <RouterLink to="/signup">회원가입</RouterLink>
      </div>

      <div class="policy-box">
        <strong>안내</strong>
        <p>사진 심사 대기나 반려 상태 계정은 로그인 후에도 승인 대기 화면에서 계속 보완과 심사 진행 상태를 확인할 수 있습니다.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken, setToken } from "../../utils/auth";

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const emailInput = ref(null);
const passwordInput = ref(null);
const form = reactive({
  email: "",
  password: "",
});

function resolveLoginErrorMessage(error) {
  const status = error.response?.status;
  const serverMessage = error.response?.data?.message;

  if (serverMessage) {
    return serverMessage;
  }

  if (status === 401) {
    return "이메일 또는 비밀번호를 다시 확인해 주세요.";
  }

  if (status === 403) {
    return "현재 로그인할 수 없는 계정 상태입니다. 운영 정책 또는 심사 상태를 확인해 주세요.";
  }

  return "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
}

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
    clearToken();
    const message = resolveLoginErrorMessage(error);
    errorMessage.value = message;
    window.alert(message);

    await nextTick();
    if (!form.email) {
      emailInput.value?.focus();
      return;
    }

    passwordInput.value?.focus();
    passwordInput.value?.select?.();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.auth-card {
  width: min(100%, 460px);
  min-height: min(860px, calc(100vh - 36px));
  margin: auto 0;
  padding: 28px 22px 24px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(255, 252, 250, 0.96)),
    rgba(255, 252, 250, 0.92);
  border: 1px solid rgba(240, 205, 190, 0.88);
  box-shadow:
    0 26px 50px rgba(98, 49, 34, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 10px;
  color: #9d4b32;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #33211d;
  font-size: clamp(1.9rem, 7vw, 2.4rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 26px;
  color: #6e534c;
  line-height: 1.62;
}

.auth-form {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input {
  border: 1px solid #e8c7bb;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 2px rgba(80, 55, 49, 0.04);
}

.primary-button {
  width: 100%;
  border: none;
  border-radius: 18px;
  padding: 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  margin-top: 10px;
  background: linear-gradient(135deg, #d45d38 0%, #ea8f64 100%);
  color: #fff;
  box-shadow: 0 16px 28px rgba(212, 93, 56, 0.26);
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.helper-row {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.helper-row a {
  color: #a54123;
  font-weight: 700;
}

.policy-box {
  margin-top: auto;
  padding: 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff7f1 0%, #fff3ec 100%);
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
  line-height: 1.6;
}

@media (min-width: 768px) {
  .auth-page {
    padding: 28px;
  }

  .auth-card {
    min-height: auto;
    padding: 36px 34px 28px;
  }
}

@media (max-width: 420px) {
  .auth-card {
    border-radius: 28px;
    padding: 24px 18px 20px;
  }

  .helper-row {
    flex-direction: column;
  }
}
</style>
