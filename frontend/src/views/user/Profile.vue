<template>
  <section class="profile-page">
    <div class="profile-shell" v-if="profile">
      <header class="page-card">
        <p class="eyebrow">Profile</p>
        <h1>내 프로필</h1>
        <p class="description">프로필 정보를 확인하고 심사 진행에 필요한 항목을 바로 수정할 수 있습니다.</p>
      </header>

      <section class="page-card">
        <div class="section-head">
          <div>
            <p class="section-label">Summary</p>
            <h2>계정 요약</h2>
          </div>
          <span class="status-pill">{{ profile.status }}</span>
        </div>

        <dl class="summary-list">
          <div><dt>이메일</dt><dd>{{ profile.email }}</dd></div>
          <div><dt>닉네임</dt><dd>{{ profile.nickname }}</dd></div>
          <div><dt>가입 방식</dt><dd>{{ providerLabel }}</dd></div>
          <div><dt>등록 사진</dt><dd>{{ imageCount }}장</dd></div>
        </dl>
      </section>

      <section class="page-card">
        <div class="section-head">
          <div>
            <p class="section-label">Edit Profile</p>
            <h2>프로필 수정</h2>
          </div>
        </div>

        <form class="profile-form" @submit.prevent="saveProfile">
          <label>
            <span>닉네임</span>
            <input v-model.trim="form.nickname" type="text" maxlength="30" required />
          </label>

          <label>
            <span>생년월일</span>
            <input v-model="form.birthDate" type="date" required />
          </label>

          <label>
            <span>성별</span>
            <select v-model="form.gender" required>
              <option value="MALE">남성</option>
              <option value="FEMALE">여성</option>
            </select>
          </label>

          <label>
            <span>거주 지역</span>
            <input v-model.trim="form.region" type="text" maxlength="50" required />
          </label>

          <label>
            <span>직업</span>
            <input v-model.trim="form.job" type="text" maxlength="100" required />
          </label>

          <label>
            <span>MBTI</span>
            <input v-model.trim="form.mbti" type="text" maxlength="4" required />
          </label>

          <label>
            <span>성격</span>
            <textarea v-model.trim="form.personality" rows="3" required></textarea>
          </label>

          <label>
            <span>이상형</span>
            <textarea v-model.trim="form.idealType" rows="3" required></textarea>
          </label>

          <label>
            <span>자기소개</span>
            <textarea v-model.trim="form.introduction" rows="4" required></textarea>
          </label>

          <div class="triple-grid">
            <label>
              <span>흡연 여부</span>
              <select v-model="form.smokingStatus" required>
                <option value="NON_SMOKER">비흡연</option>
                <option value="OCCASIONAL">가끔 흡연</option>
                <option value="SMOKER">흡연</option>
              </select>
            </label>

            <label>
              <span>음주 여부</span>
              <select v-model="form.drinkingStatus" required>
                <option value="NONE">비음주</option>
                <option value="SOMETIMES">가끔 음주</option>
                <option value="OFTEN">자주 음주</option>
              </select>
            </label>

            <label>
              <span>종교</span>
              <select v-model="form.religion" required>
                <option value="NONE">없음</option>
                <option value="CHRISTIAN">기독교</option>
                <option value="CATHOLIC">천주교</option>
                <option value="BUDDHIST">불교</option>
                <option value="OTHER">기타</option>
              </select>
            </label>
          </div>

          <button class="primary-button" :disabled="saving">{{ saving ? '저장 중...' : '프로필 저장' }}</button>
        </form>

        <p v-if="saveMessage" class="message success">{{ saveMessage }}</p>
        <p v-if="saveError" class="message error">{{ saveError }}</p>
      </section>

      <section class="card-grid">
        <article class="page-card">
          <p class="section-label">Review</p>
          <h2>심사 진행</h2>
          <p class="description">사진 등록과 심사 상태는 심사 대기 화면에서 계속 이어집니다.</p>
          <button class="ghost-button" type="button" @click="goToReview">심사 화면으로 이동</button>
        </article>

        <article class="page-card">
          <p class="section-label">Account</p>
          <h2>계정 관리</h2>
          <p class="description">현재 계정에서 로그아웃하고 다시 로그인할 수 있습니다.</p>
          <button class="logout-button" type="button" @click="logout">로그아웃</button>
        </article>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const saving = ref(false);
const saveMessage = ref("");
const saveError = ref("");
const profile = ref(null);
const imageCount = ref(0);

const form = reactive({
  nickname: "",
  birthDate: "",
  gender: "MALE",
  region: "",
  job: "",
  mbti: "",
  personality: "",
  idealType: "",
  introduction: "",
  smokingStatus: "NON_SMOKER",
  drinkingStatus: "NONE",
  religion: "NONE",
});

const providerLabel = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "PHONE") return "전화번호";
  return "자체 이메일";
});

function hydrateForm(data) {
  profile.value = data;
  form.nickname = data.nickname || "";
  form.birthDate = data.birthDate || "";
  form.gender = data.gender || "MALE";
  form.region = data.region || "";
  form.job = data.job || "";
  form.mbti = data.mbti || "";
  form.personality = data.personality || "";
  form.idealType = data.idealType || "";
  form.introduction = data.introduction || "";
  form.smokingStatus = data.smokingStatus || "NON_SMOKER";
  form.drinkingStatus = data.drinkingStatus || "NONE";
  form.religion = data.religion || "NONE";
}

async function fetchProfileWithFallback() {
  try {
    const { data } = await api.get("/profile/me");
    return data;
  } catch (error) {
    if (error.response?.status !== 404) {
      throw error;
    }

    const { data } = await api.get("/user/me");
    return {
      ...data,
      birthDate: "",
      gender: "MALE",
      region: "",
      job: "",
      mbti: "",
      personality: "",
      idealType: "",
      introduction: "",
      smokingStatus: "NON_SMOKER",
      drinkingStatus: "NONE",
      religion: "NONE",
    };
  }
}

async function loadProfile() {
  try {
    const [myProfile, { data: images }] = await Promise.all([
      fetchProfileWithFallback(),
      api.get("/profile-images/me"),
    ]);

    hydrateForm(myProfile);
    imageCount.value = images.length;
  } catch (error) {
    clearToken();
    router.replace("/");
  }
}

function validateProfile() {
  if (!form.nickname.trim()) return "닉네임을 입력해주세요.";
  if (!form.birthDate) return "생년월일을 입력해주세요.";
  if (!form.gender) return "성별을 선택해주세요.";
  if (!form.region.trim()) return "거주 지역을 입력해주세요.";
  if (!form.job.trim()) return "직업을 입력해주세요.";
  if (!form.mbti.trim()) return "MBTI를 입력해주세요.";
  if (!form.personality.trim()) return "성격을 입력해주세요.";
  if (!form.idealType.trim()) return "이상형을 입력해주세요.";
  if (!form.introduction.trim()) return "자기소개를 입력해주세요.";
  if (!form.smokingStatus) return "흡연 여부를 선택해주세요.";
  if (!form.drinkingStatus) return "음주 여부를 선택해주세요.";
  if (!form.religion) return "종교를 선택해주세요.";
  return "";
}

async function saveProfile() {
  saveMessage.value = "";
  saveError.value = "";

  const validationMessage = validateProfile();
  if (validationMessage) {
    saveError.value = validationMessage;
    return;
  }

  saving.value = true;

  try {
    const payload = {
      ...form,
      nickname: form.nickname.trim(),
      region: form.region.trim(),
      job: form.job.trim(),
      mbti: form.mbti.trim(),
      personality: form.personality.trim(),
      idealType: form.idealType.trim(),
      introduction: form.introduction.trim(),
    };

    const { data } = await api.put("/profile/me", payload);
    hydrateForm(data);
    saveMessage.value = "프로필을 저장했어요. 다음 단계에서 심사용 사진을 등록해 주세요.";
    if (data.status !== "ACTIVE") {
      router.push("/review-pending");
    }
  } catch (error) {
    saveError.value = error.response?.data?.message || "프로필 저장에 실패했습니다.";
  } finally {
    saving.value = false;
  }
}

function goToReview() {
  router.push("/review-pending");
}

function logout() {
  clearToken();
  router.replace("/");
}

onMounted(loadProfile);
</script>

<style scoped>
.profile-page { min-height: 100vh; padding: max(16px, env(safe-area-inset-top)) 16px 24px; display: flex; justify-content: center; }
.profile-shell { width: min(100%, 720px); display: grid; gap: 16px; }
.page-card {
  padding: 24px 20px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
}
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1, h2 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.status-pill { padding: 8px 12px; border-radius: 999px; background: #fff1e8; color: #9d5a43; font-size: 12px; font-weight: 800; }
.summary-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin: 0; }
.summary-list div { padding: 14px; border-radius: 18px; background: rgba(255, 247, 242, 0.9); }
.summary-list dt { color: #8b675b; font-size: 13px; }
.summary-list dd { margin: 8px 0 0; color: #2f211d; font-weight: 700; }
.profile-form { display: grid; gap: 14px; }
label { display: grid; gap: 8px; color: #543932; font-weight: 600; }
input, select, textarea { border: 1px solid #e5c2b2; border-radius: 16px; padding: 14px 16px; font-size: 15px; background: rgba(255,255,255,0.92); font-family: inherit; }
textarea { resize: vertical; }
.triple-grid, .card-grid { display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.triple-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.primary-button, .ghost-button, .logout-button { border-radius: 999px; padding: 12px 18px; font-size: 14px; font-weight: 700; cursor: pointer; }
.primary-button { border: none; background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%); color: #fff; }
.ghost-button { border: 1px solid rgba(207, 150, 128, 0.36); background: rgba(255,248,243,0.82); color: #6a4137; }
.logout-button { border: none; background: #f3dfd7; color: #7a473c; }
.message { margin-top: 14px; font-weight: 700; }
.success { color: #1f7a43; }
.error { color: #b72f2f; }
@media (max-width: 760px) { .summary-list, .triple-grid, .card-grid { grid-template-columns: 1fr; } .section-head { flex-direction: column; align-items: flex-start; } }
</style>
