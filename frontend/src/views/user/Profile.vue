<template>
  <section class="profile-page">
    <div class="profile-shell">
      <header class="page-card">
        <p class="eyebrow">Profile</p>
        <h1>내 프로필</h1>
        <p class="description">
          프로필을 수정하고, 안전센터와 설정, 로그아웃까지 한 곳에서 관리할 수 있게 구성했습니다.
        </p>
      </header>

      <section class="page-card">
        <div class="section-head">
          <div>
            <p class="section-label">Edit Profile</p>
            <h2>프로필 수정</h2>
          </div>
          <span class="section-caption">기본 정보와 소개 문구를 바로 수정할 수 있어요.</span>
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

          <div class="region-grid">
            <label>
              <span>거주 시/도</span>
              <select v-model="selectedRegionCity" @change="handleRegionCityChange" required>
                <option value="">시/도 선택</option>
                <option v-for="city in regionCities" :key="city" :value="city">{{ city }}</option>
              </select>
            </label>

            <label>
              <span>거주 구/군</span>
              <select v-model="selectedRegionDistrict" @change="updateRegion" :disabled="!selectedRegionCity" required>
                <option value="">구/군 선택</option>
                <option v-for="district in availableDistricts" :key="district" :value="district">{{ district }}</option>
              </select>
            </label>
          </div>

          <label>
            <span>직업</span>
            <input v-model.trim="form.job" type="text" maxlength="100" />
          </label>

          <label>
            <span>MBTI</span>
            <select v-model="form.mbti">
              <option value="">선택 안 함</option>
              <option v-for="item in mbtiOptions" :key="item" :value="item">{{ item }}</option>
            </select>
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

          <label>
            <span>성격</span>
            <textarea v-model.trim="form.personality" rows="3" />
          </label>

          <label>
            <span>이상형</span>
            <textarea v-model.trim="form.idealType" rows="3" />
          </label>

          <label>
            <span>자기소개</span>
            <textarea v-model.trim="form.introduction" rows="4" />
          </label>

          <button class="primary-button" :disabled="saving">
            {{ saving ? "저장 중..." : "프로필 저장" }}
          </button>
        </form>

        <p v-if="saveMessage" class="message success">{{ saveMessage }}</p>
        <p v-if="saveError" class="message error">{{ saveError }}</p>
      </section>

      <section class="card-grid">
        <article class="page-card">
          <div class="section-head compact">
            <div>
              <p class="section-label">Safety Center</p>
              <h2>안전센터</h2>
            </div>
          </div>

          <ul class="tip-list">
            <li>투자, 코인, 수익 보장 이야기를 빠르게 꺼내는 경우</li>
            <li>라인, 텔레그램 같은 외부 메신저로 바로 이동을 유도하는 경우</li>
            <li>송금이나 해외 계좌 관련 요청을 하는 경우</li>
            <li>감정적인 친밀감을 만든 뒤 금전 이야기를 결합하는 경우</li>
          </ul>

          <form class="report-form" @submit.prevent="submitReport">
            <label>
              <span>신고 대상 사용자 ID</span>
              <input v-model.number="reportForm.reportedUserId" type="number" min="1" placeholder="예: 12" />
            </label>

            <label>
              <span>신고 사유</span>
              <select v-model="reportForm.reasonCode">
                <option value="INVESTMENT">투자/금전 유도</option>
                <option value="EXTERNAL_CONTACT">외부 메신저 유도</option>
                <option value="IMPERSONATION">사칭 의심</option>
                <option value="HARASSMENT">불쾌한 언행</option>
                <option value="OTHER">기타</option>
              </select>
            </label>

            <label>
              <span>상세 설명</span>
              <textarea
                v-model.trim="reportForm.detail"
                rows="4"
                placeholder="어떤 표현이나 행동이 문제였는지 구체적으로 적어 주세요."
              />
            </label>

            <button class="secondary-button" :disabled="reportLoading">
              {{ reportLoading ? "신고 접수 중..." : "신고 접수" }}
            </button>
          </form>
        </article>

        <article class="page-card">
          <div class="section-head compact">
            <div>
              <p class="section-label">Settings</p>
              <h2>설정</h2>
            </div>
          </div>

          <dl class="settings-list">
            <div>
              <dt>계정 상태</dt>
              <dd>{{ profile?.status || "-" }}</dd>
            </div>
            <div>
              <dt>연결 로그인</dt>
              <dd>{{ providerLabel }}</dd>
            </div>
            <div>
              <dt>등록 사진</dt>
              <dd>{{ imageCount }}장</dd>
            </div>
            <div>
              <dt>안전 점수</dt>
              <dd>{{ fraudRiskScore }}</dd>
            </div>
          </dl>

          <button class="ghost-button" type="button" @click="goToReview">사진 심사 화면 보기</button>
          <button class="logout-button" type="button" @click="logout">로그아웃</button>
        </article>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const mbtiOptions = [
  "INTJ", "INTP", "ENTJ", "ENTP",
  "INFJ", "INFP", "ENFJ", "ENFP",
  "ISTJ", "ISFJ", "ESTJ", "ESFJ",
  "ISTP", "ISFP", "ESTP", "ESFP",
];

const regionOptions = {
  서울: ["강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"],
  경기: ["고양시", "과천시", "광명시", "광주시", "구리시", "구리시", "김포시", "남양주시", "성남시", "수원시", "시흥시", "안산시", "안양시", "용인시", "의정부시", "파주시", "평택시", "하남시", "화성시"],
  인천: ["계양구", "남동구", "미추홀구", "부평구", "서구", "연수구", "중구"],
  부산: ["강서구", "금정구", "남구", "동래구", "부산진구", "북구", "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구"],
  대구: ["남구", "달서구", "달성군", "동구", "북구", "서구", "수성구", "중구"],
  대전: ["대덕구", "동구", "서구", "유성구", "중구"],
  광주: ["광산구", "남구", "동구", "북구", "서구"],
  울산: ["남구", "동구", "북구", "중구", "울주군"],
  세종: ["세종시"],
  강원: ["강릉시", "동해시", "속초시", "원주시", "춘천시"],
  충북: ["제천시", "청주시", "충주시"],
  충남: ["공주시", "논산시", "당진시", "보령시", "서산시", "아산시", "천안시"],
  전북: ["군산시", "김제시", "남원시", "익산시", "전주시", "정읍시"],
  전남: ["광양시", "목포시", "순천시", "여수시"],
  경북: ["경산시", "경주시", "구미시", "김천시", "포항시"],
  경남: ["거제시", "김해시", "밀양시", "양산시", "진주시", "창원시", "통영시"],
  제주: ["제주시", "서귀포시"],
};

const router = useRouter();
const saving = ref(false);
const reportLoading = ref(false);
const saveMessage = ref("");
const saveError = ref("");
const profile = ref(null);
const imageCount = ref(0);
const fraudRiskScore = ref("-");
const selectedRegionCity = ref("");
const selectedRegionDistrict = ref("");

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

const reportForm = reactive({
  reportedUserId: null,
  reasonCode: "INVESTMENT",
  detail: "",
});

const regionCities = Object.keys(regionOptions);
const availableDistricts = computed(() => regionOptions[selectedRegionCity.value] || []);

const providerLabel = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "BOTH") return "이메일 + Google";
  if (provider === "GOOGLE") return "Google";
  return "이메일";
});

const updateRegion = () => {
  form.region = selectedRegionCity.value && selectedRegionDistrict.value
    ? `${selectedRegionCity.value} ${selectedRegionDistrict.value}`
    : "";
};

const handleRegionCityChange = () => {
  selectedRegionDistrict.value = "";
  updateRegion();
};

const hydrateForm = (data) => {
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

  const matchedCity = regionCities.find((city) => data.region?.startsWith(city));
  if (matchedCity) {
    selectedRegionCity.value = matchedCity;
    selectedRegionDistrict.value = data.region.replace(`${matchedCity} `, "");
  } else {
    selectedRegionCity.value = "";
    selectedRegionDistrict.value = "";
  }
};

const loadProfile = async () => {
  try {
    const [{ data: myProfile }, { data: images }, { data: me }] = await Promise.all([
      api.get("/profile/me"),
      api.get("/profile-images/me"),
      api.get("/user/me"),
    ]);

    hydrateForm(myProfile);
    imageCount.value = images.length;
    fraudRiskScore.value = me.fraudRiskScore ?? "-";
  } catch (error) {
    clearToken();
    router.replace("/");
  }
};

const validateProfile = () => {
  if (!form.nickname.trim()) return "닉네임을 입력해 주세요.";
  if (!form.birthDate) return "생년월일을 입력해 주세요.";
  if (!selectedRegionCity.value || !selectedRegionDistrict.value) return "거주 지역을 선택해 주세요.";
  return "";
};

const saveProfile = async () => {
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
      job: form.job.trim(),
      personality: form.personality.trim(),
      idealType: form.idealType.trim(),
      introduction: form.introduction.trim(),
      region: form.region,
    };

    const { data } = await api.put("/profile/me", payload);
    hydrateForm(data);
    saveMessage.value = "프로필을 저장했어요.";
  } catch (error) {
    saveError.value = error.response?.data?.message || "프로필 저장에 실패했습니다.";
  } finally {
    saving.value = false;
  }
};

const submitReport = async () => {
  if (!reportForm.reportedUserId) {
    window.alert("신고 대상 사용자 ID를 입력해 주세요.");
    return;
  }

  if (!reportForm.detail) {
    window.alert("상세 설명을 입력해 주세요.");
    return;
  }

  reportLoading.value = true;

  try {
    const { data } = await api.post("/safety/reports", reportForm);
    window.alert(`신고가 접수되었습니다. 접수 번호: ${data.id}`);
    reportForm.reportedUserId = null;
    reportForm.reasonCode = "INVESTMENT";
    reportForm.detail = "";
  } catch (error) {
    window.alert(error.response?.data?.message || "신고 접수에 실패했습니다.");
  } finally {
    reportLoading.value = false;
  }
};

const goToReview = () => {
  router.push("/review-pending");
};

const logout = () => {
  clearToken();
  router.replace("/");
};

onMounted(loadProfile);
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: max(12px, env(safe-area-inset-top)) 12px 24px;
}

.profile-shell {
  width: min(100%, 480px);
  margin: 0 auto;
}

.page-card {
  padding: 20px 16px;
  border-radius: 30px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 22px 44px rgba(109, 57, 41, 0.08);
}

.page-card + .page-card,
.card-grid,
.message {
  margin-top: 14px;
}

.eyebrow,
.section-label {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1,
h2,
strong {
  color: #2f211d;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 8vw, 2.7rem);
  line-height: 1.06;
}

h2 {
  margin: 0;
  font-size: clamp(1.35rem, 5vw, 1.8rem);
}

.description,
.section-caption,
.tip-list li {
  color: #6f564d;
  line-height: 1.68;
}

.section-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-form,
.region-grid,
.triple-grid,
.card-grid,
.report-form {
  display: grid;
  gap: 14px;
}

.profile-form {
  margin-top: 18px;
}

.region-grid,
.triple-grid,
.card-grid {
  grid-template-columns: 1fr;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #e8c7bb;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 2px rgba(80, 55, 49, 0.04);
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.primary-button,
.secondary-button,
.ghost-button,
.logout-button {
  width: 100%;
  border: none;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  background: linear-gradient(135deg, #d45d38 0%, #ea8f64 100%);
  color: #fff;
  box-shadow: 0 16px 28px rgba(212, 93, 56, 0.24);
}

.secondary-button {
  background: rgba(255, 244, 239, 0.92);
  color: #8f4e38;
}

.ghost-button {
  margin-top: 18px;
  background: rgba(255, 248, 243, 0.9);
  color: #6a4137;
  border: 1px solid rgba(207, 150, 128, 0.36);
}

.logout-button {
  margin-top: 10px;
  background: #34211d;
  color: #fff;
}

.message {
  padding: 14px 16px;
  border-radius: 20px;
  font-weight: 600;
}

.success {
  background: rgba(232, 248, 238, 0.9);
  color: #176540;
}

.error {
  background: rgba(255, 236, 236, 0.94);
  color: #8c2f2f;
}

.tip-list {
  margin: 0;
  padding-left: 18px;
}

.report-form {
  margin-top: 18px;
}

.settings-list {
  display: grid;
  gap: 14px;
  margin: 0;
}

.settings-list div {
  padding: 14px 0;
  border-bottom: 1px solid rgba(235, 209, 199, 0.85);
}

.settings-list div:last-child {
  border-bottom: none;
}

.settings-list dt {
  color: #916456;
  font-size: 13px;
  font-weight: 700;
}

.settings-list dd {
  margin: 8px 0 0;
  color: #33241f;
  font-weight: 700;
}

@media (min-width: 768px) {
  .profile-page {
    padding: 24px 20px 40px;
  }

  .profile-shell {
    width: min(1180px, 100%);
  }

  .page-card {
    padding: 26px;
  }

  .region-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .triple-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
