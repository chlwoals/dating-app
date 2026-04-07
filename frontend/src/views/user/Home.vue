<template>
  <section class="home-page">
    <div class="home-shell" v-if="profile && userSummary">
      <header class="topbar">
        <div>
          <p class="eyebrow">Dating Lounge</p>
          <h1>{{ profile.nickname }}님, 오늘의 인연을 둘러보세요</h1>
          <p class="subtitle">
            메인 화면에서는 추천 카드, 빠른 반응, 안전 안내와 신고까지 한 번에 확인할 수 있습니다.
          </p>
        </div>

        <div class="topbar-actions">
          <button class="ghost-button" @click="scrollToSection('discover-section')">추천 보기</button>
          <button class="ghost-button" @click="scrollToSection('safety-section')">안전센터</button>
          <button class="primary-button" @click="scrollToSection('profile-section')">내 프로필</button>
          <button class="logout-button" @click="logout">로그아웃</button>
        </div>
      </header>

      <section v-if="showWatchBanner" class="safety-banner warning-banner">
        <strong>주의 안내</strong>
        <p>
          프로필 문구에서 투자·송금·외부 메신저 유도와 비슷한 표현이 감지되었습니다.
          운영 정책 위반이 아니라면 프로필 문구를 한 번 더 확인해주세요.
        </p>
      </section>

      <section class="hero-panel">
        <div class="hero-copy">
          <span class="status-pill" :class="statusClass">{{ statusLabel }}</span>
          <h2>{{ heroTitle }}</h2>
          <p>{{ heroDescription }}</p>

          <div class="hero-highlights">
            <article class="highlight-card">
              <span>프로필 완성도</span>
              <strong>{{ profileCompletion }}%</strong>
              <small>직업, MBTI, 자기소개, 이상형, 성격 기준</small>
            </article>
            <article class="highlight-card">
              <span>등록 사진</span>
              <strong>{{ images.length }}장</strong>
              <small>대표 사진 포함 최대 5장까지 등록할 수 있어요.</small>
            </article>
            <article class="highlight-card">
              <span>계정 연결</span>
              <strong>{{ providerLabel }}</strong>
              <small>{{ providerMessage }}</small>
            </article>
          </div>
        </div>

        <div class="hero-visual">
          <div class="profile-frame">
            <img v-if="mainImage" :src="resolveImageUrl(mainImage.imageUrl)" :alt="`${profile.nickname} 대표 사진`" />
            <div v-else class="profile-placeholder">
              <span>{{ profile.nickname.slice(0, 1) }}</span>
              <p>대표 사진을 등록하면 메인 화면의 완성도가 더 좋아집니다.</p>
            </div>
          </div>

          <div class="mini-profile">
            <strong>{{ profile.nickname }}</strong>
            <p>{{ profile.region }} · {{ profile.job || '직업 미입력' }}</p>
            <div class="chip-row">
              <span class="chip">{{ profile.gender === 'MALE' ? '남성' : '여성' }}</span>
              <span class="chip">{{ profile.mbti || 'MBTI 미입력' }}</span>
              <span class="chip">{{ ageLabel }}</span>
            </div>
          </div>
        </div>
      </section>

      <section id="discover-section" class="section-block">
        <div class="section-head">
          <div>
            <p class="section-label">Discover</p>
            <h3>오늘의 추천 카드</h3>
          </div>
          <span class="section-caption">현재는 시안형 카드이며, 실제 추천 API가 붙으면 그대로 교체할 수 있습니다.</span>
        </div>

        <div class="discover-grid">
          <article v-for="card in recommendationCards" :key="card.id" class="discover-card">
            <div class="discover-photo" :style="{ background: card.background }">
              <div class="discover-overlay">
                <span>{{ card.badge }}</span>
                <strong>{{ card.name }}, {{ card.age }}</strong>
              </div>
            </div>

            <div class="discover-body">
              <div class="discover-meta">
                <span>{{ card.region }}</span>
                <span>{{ card.job }}</span>
              </div>
              <p>{{ card.introduction }}</p>
              <div class="tag-row">
                <span v-for="tag in card.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>

              <div class="action-row">
                <button class="action-button pass" @click="markReaction(card.id, 'pass')">패스</button>
                <button class="action-button detail" @click="selectedCard = card">상세 보기</button>
                <button class="action-button like" @click="markReaction(card.id, 'like')">좋아요</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="section-block match-section">
        <div class="section-head compact">
          <div>
            <p class="section-label">Preview</p>
            <h3>매칭과 대화 미리보기</h3>
          </div>
        </div>

        <div class="match-grid">
          <article class="match-card active-match">
            <span class="match-badge">최근 반응</span>
            <strong>{{ likedCount }}명에게 좋아요를 보냈어요</strong>
            <p>추천 카드와 매칭 흐름은 이후 실제 매칭 API와 자연스럽게 연결될 수 있도록 구성했습니다.</p>
          </article>

          <article class="match-card queue-card">
            <span class="match-badge calm">오늘의 상태</span>
            <ul class="queue-list">
              <li>
                <strong>{{ likedCount }}명</strong>
                <span>좋아요를 보낸 카드</span>
              </li>
              <li>
                <strong>{{ passedCount }}명</strong>
                <span>패스한 카드</span>
              </li>
              <li>
                <strong>{{ userSummary.fraudRiskScore }}</strong>
                <span>현재 안전 점수 감시 상태</span>
              </li>
            </ul>
          </article>
        </div>
      </section>

      <section id="safety-section" class="section-block safety-section">
        <div class="section-head compact">
          <div>
            <p class="section-label">Safety Center</p>
            <h3>로맨스 스캠 안전센터</h3>
          </div>
        </div>

        <div class="safety-grid">
          <article class="info-card tip-card">
            <h4>주의해야 할 신호</h4>
            <ul class="tip-list">
              <li>투자, 코인, 수익 보장, 리딩방 같은 이야기를 빠르게 꺼내는 경우</li>
              <li>텔레그램, 라인, 오픈채팅 등 외부 메신저로 바로 이동을 유도하는 경우</li>
              <li>송금, 환전, 해외 계좌, 입금을 부탁하는 경우</li>
              <li>감정 형성을 서두르면서 금전 이야기를 결합하는 경우</li>
            </ul>
          </article>

          <article class="info-card report-card">
            <h4>의심 계정 신고</h4>
            <p class="helper-copy">
              실제 상대 프로필/채팅 화면이 붙기 전까지는 여기서 사용자 ID 기준으로 신고할 수 있습니다.
            </p>

            <label>
              <span>신고 대상 사용자 ID</span>
              <input v-model.number="reportForm.reportedUserId" type="number" min="1" placeholder="예: 12" />
            </label>

            <label>
              <span>신고 사유</span>
              <select v-model="reportForm.reasonCode">
                <option value="INVESTMENT">투자·금전 유도</option>
                <option value="EXTERNAL_CONTACT">외부 메신저 유도</option>
                <option value="IMPERSONATION">사칭 의심</option>
                <option value="HARASSMENT">부적절한 접근</option>
                <option value="OTHER">기타</option>
              </select>
            </label>

            <label>
              <span>상세 설명</span>
              <textarea
                v-model.trim="reportForm.detail"
                rows="4"
                placeholder="어떤 표현이나 행동이 의심스러웠는지 구체적으로 적어주세요."
              ></textarea>
            </label>

            <div class="report-actions">
              <button class="primary-button" @click="submitReport" :disabled="reportLoading">
                {{ reportLoading ? '신고 접수 중...' : '신고 접수' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section id="profile-section" class="section-block profile-layout">
        <div class="section-head compact">
          <div>
            <p class="section-label">Profile Snapshot</p>
            <h3>내 프로필 한눈에 보기</h3>
          </div>
        </div>

        <div class="profile-layout-grid">
          <article class="info-card">
            <h4>기본 정보</h4>
            <dl>
              <div>
                <dt>이메일</dt>
                <dd>{{ profile.email }}</dd>
              </div>
              <div>
                <dt>거주 지역</dt>
                <dd>{{ profile.region }}</dd>
              </div>
              <div>
                <dt>직업</dt>
                <dd>{{ profile.job || '아직 입력 전' }}</dd>
              </div>
              <div>
                <dt>MBTI</dt>
                <dd>{{ profile.mbti || '아직 입력 전' }}</dd>
              </div>
            </dl>
          </article>

          <article class="info-card">
            <h4>자기소개</h4>
            <p class="body-copy">{{ profile.introduction || '아직 자기소개가 등록되지 않았습니다.' }}</p>
            <h4>성격</h4>
            <p class="body-copy">{{ profile.personality || '성격 소개를 추가하면 더 잘 맞는 추천을 만들 수 있어요.' }}</p>
          </article>

          <article class="info-card">
            <h4>이상형과 생활 정보</h4>
            <p class="body-copy">{{ profile.idealType || '이상형 설명을 입력하면 추천 정확도를 높일 수 있습니다.' }}</p>
            <div class="tag-row spacious">
              <span class="tag soft">{{ smokingLabel }}</span>
              <span class="tag soft">{{ drinkingLabel }}</span>
              <span class="tag soft">{{ religionLabel }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>

    <div v-else class="loading-shell">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>메인 화면을 준비하고 있어요</h2>
        <p>프로필과 사진 정보를 불러오는 중입니다.</p>
      </div>
    </div>

    <div v-if="selectedCard" class="modal-backdrop" @click.self="selectedCard = null">
      <div class="modal-card">
        <div class="modal-photo" :style="{ background: selectedCard.background }">
          <span>{{ selectedCard.badge }}</span>
          <strong>{{ selectedCard.name }}, {{ selectedCard.age }}</strong>
        </div>
        <div class="modal-body">
          <h3>{{ selectedCard.name }}님의 카드</h3>
          <p>{{ selectedCard.introduction }}</p>
          <div class="tag-row spacious">
            <span v-for="tag in selectedCard.tags" :key="tag" class="tag soft">{{ tag }}</span>
          </div>
          <div class="modal-list">
            <div>
              <dt>활동 지역</dt>
              <dd>{{ selectedCard.region }}</dd>
            </div>
            <div>
              <dt>직업</dt>
              <dd>{{ selectedCard.job }}</dd>
            </div>
            <div>
              <dt>추천 이유</dt>
              <dd>{{ selectedCard.reason }}</dd>
            </div>
          </div>
          <div class="action-row modal-actions">
            <button class="action-button pass" @click="markReaction(selectedCard.id, 'pass'); selectedCard = null">패스</button>
            <button class="action-button like" @click="markReaction(selectedCard.id, 'like'); selectedCard = null">좋아요</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const userSummary = ref(null);
const profile = ref(null);
const images = ref([]);
const selectedCard = ref(null);
const reactions = ref({});
const reportLoading = ref(false);

const reportForm = ref({
  reportedUserId: null,
  reasonCode: "INVESTMENT",
  detail: "",
});

const mainImage = computed(() => images.value.find((image) => image.isMain) || images.value[0] || null);
const likedCount = computed(() => Object.values(reactions.value).filter((value) => value === "like").length);
const passedCount = computed(() => Object.values(reactions.value).filter((value) => value === "pass").length);
const showWatchBanner = computed(() => userSummary.value?.fraudReviewStatus === "WATCH");

const profileCompletion = computed(() => {
  if (!profile.value) return 0;
  const fields = [profile.value.job, profile.value.mbti, profile.value.personality, profile.value.idealType, profile.value.introduction];
  const completed = fields.filter((value) => value && String(value).trim()).length;
  return Math.round((completed / fields.length) * 100);
});

const providerLabel = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "BOTH") return "이메일 + Google";
  if (provider === "GOOGLE") return "Google";
  return "이메일";
});

const providerMessage = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "BOTH") return "두 로그인 방식을 모두 사용할 수 있어요.";
  if (provider === "GOOGLE") return "현재는 Google 계정과 연결되어 있어요.";
  return "현재는 이메일 로그인으로 연결되어 있어요.";
});

const statusLabel = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "이용 가능";
  if (status === "PENDING_REVIEW") return "심사 진행 중";
  if (status === "REJECTED") return "보완 필요";
  if (status === "SUSPENDED") return "이용 제한";
  return "상태 확인 중";
});

const statusClass = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "status-active";
  if (status === "PENDING_REVIEW") return "status-pending";
  if (status === "REJECTED") return "status-rejected";
  if (status === "SUSPENDED") return "status-suspended";
  return "";
});

const ageLabel = computed(() => {
  if (!profile.value?.birthDate) return "나이 미입력";
  const birthYear = Number(String(profile.value.birthDate).slice(0, 4));
  return `${new Date().getFullYear() - birthYear + 1}세`;
});

const heroTitle = computed(() => {
  if (!profile.value) return "오늘의 추천을 준비하고 있어요";
  return `${profile.value.region}에서 잘 맞는 인연을 찾을 준비가 되었어요`;
});

const heroDescription = computed(() => {
  if (!profile.value) return "프로필을 바탕으로 메인 화면을 구성하고 있습니다.";
  return `${profile.value.mbti ? `${profile.value.mbti} 성향과 ` : ""}${profile.value.introduction ? "자기소개를 바탕으로" : "기본 프로필을 바탕으로"} 메인 분위기를 맞췄습니다.`;
});

const smokingLabel = computed(() => ({ NON_SMOKER: "비흡연", SMOKER: "흡연", OCCASIONAL: "가끔 흡연" }[profile.value?.smokingStatus] || "흡연 정보 미입력"));
const drinkingLabel = computed(() => ({ NONE: "술 안 마심", SOMETIMES: "가끔 음주", OFTEN: "자주 음주" }[profile.value?.drinkingStatus] || "음주 정보 미입력"));
const religionLabel = computed(() => ({ NONE: "무교", CHRISTIAN: "기독교", BUDDHIST: "불교", CATHOLIC: "천주교", OTHER: "기타 종교" }[profile.value?.religion] || "종교 정보 미입력"));

const recommendationCards = computed(() => {
  if (!profile.value) return [];

  const region = profile.value.region || "서울";
  const ideal = profile.value.idealType || "대화가 잘 통하는 사람";
  const mbti = profile.value.mbti || "감성형";

  return [
    {
      id: 1,
      name: "서윤",
      age: 29,
      region,
      job: "브랜드 디자이너",
      badge: "오늘 가장 잘 맞는 카드",
      introduction: `${ideal}을 중요하게 보는 분위기를 반영해 차분하게 대화가 이어질 타입으로 구성한 추천 카드예요.`,
      tags: [mbti, "주말 데이트", "카페 좋아해요"],
      reason: "이상형 문장과 자기소개 톤이 비슷해서 첫 카드로 배치했어요.",
      background: "linear-gradient(135deg, #f5b490 0%, #f8d6be 100%)",
    },
    {
      id: 2,
      name: "민지",
      age: 31,
      region: `${region.split(" ")[0]} 근처`,
      job: "콘텐츠 마케터",
      badge: "대화 템포가 맞을 타입",
      introduction: "자기소개 길이와 생활 패턴을 기준으로 편안한 대화가 가능한 카드로 준비했어요.",
      tags: [drinkingLabel.value, "산책", "사진 취향"],
      reason: "라이프스타일 정보가 현재 프로필과 비교적 잘 맞는 카드예요.",
      background: "linear-gradient(135deg, #ef8c74 0%, #f3c3b2 100%)",
    },
    {
      id: 3,
      name: "지안",
      age: 27,
      region: "같은 생활권",
      job: "서비스 기획자",
      badge: "프로필 완성도 기반 추천",
      introduction: "직업, MBTI, 자기소개가 채워질수록 이런 메인 카드가 실제 추천 결과로 더 정교해질 수 있어요.",
      tags: [smokingLabel.value, religionLabel.value, "깊은 대화"],
      reason: "현재 프로필 정보가 반영되면 어떤 유형의 추천이 나올지 보여주는 카드입니다.",
      background: "linear-gradient(135deg, #d9826b 0%, #f2b8a3 100%)",
    },
  ];
});

onMounted(async () => {
  try {
    const [{ data: me }, { data: myProfile }, { data: myImages }] = await Promise.all([
      api.get("/user/me"),
      api.get("/profile/me"),
      api.get("/profile-images/me"),
    ]);

    if (me.status === "SUSPENDED" || me.fraudReviewStatus === "HIGH_RISK") {
      alert("위험 활동이 감지되어 계정 이용이 일시 제한되었습니다. 운영 검토 후 다시 안내드릴게요.");
      clearToken();
      router.replace("/");
      return;
    }

    if (me.status !== "ACTIVE") {
      router.replace("/review-pending");
      return;
    }

    userSummary.value = me;
    profile.value = myProfile;
    images.value = myImages;
  } catch (error) {
    clearToken();
    router.replace("/");
  }
});

function resolveImageUrl(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) return imageUrl;
  return `http://${window.location.hostname}:8080${imageUrl}`;
}

function scrollToSection(sectionId) {
  document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function markReaction(cardId, type) {
  reactions.value = {
    ...reactions.value,
    [cardId]: type,
  };
}

async function submitReport() {
  if (!reportForm.value.reportedUserId) {
    alert("신고 대상 사용자 ID를 입력해주세요.");
    return;
  }

  if (!reportForm.value.detail) {
    alert("상세 설명을 입력해주세요.");
    return;
  }

  reportLoading.value = true;
  try {
    const { data } = await api.post("/safety/reports", reportForm.value);
    alert(`신고가 접수되었습니다. 접수 번호: ${data.id}`);
    reportForm.value = {
      reportedUserId: null,
      reasonCode: "INVESTMENT",
      detail: "",
    };
  } catch (error) {
    alert(error.response?.data?.message || "신고 접수에 실패했습니다.");
  } finally {
    reportLoading.value = false;
  }
}

function logout() {
  clearToken();
  router.replace("/");
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 10%, rgba(255, 214, 191, 0.95) 0%, rgba(255, 214, 191, 0) 30%),
    radial-gradient(circle at 80% 0%, rgba(241, 153, 118, 0.55) 0%, rgba(241, 153, 118, 0) 32%),
    linear-gradient(180deg, #fff8f3 0%, #fdebe2 42%, #f6d2bf 100%);
  padding: 40px 20px 72px;
  font-family: "SUIT Variable", "Pretendard", sans-serif;
}

.home-shell {
  width: min(1180px, 100%);
  margin: 0 auto;
}

.topbar,
.section-head,
.topbar-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.eyebrow,
.section-label {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1,
h2,
h3,
h4,
strong {
  color: #2f211d;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.2rem);
  line-height: 1.08;
}

.subtitle,
.section-caption,
.body-copy,
.helper-copy,
.tip-list li {
  color: #6f564d;
  line-height: 1.65;
}

.primary-button,
.ghost-button,
.logout-button,
.action-button {
  border: none;
  border-radius: 999px;
  padding: 13px 18px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%);
  color: #fff;
}

.ghost-button {
  background: rgba(255, 248, 243, 0.82);
  color: #6a4137;
  border: 1px solid rgba(207, 150, 128, 0.36);
}

.logout-button {
  background: #34211d;
  color: #fff;
}

.safety-banner {
  margin-top: 18px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid #efc1b8;
}

.warning-banner {
  background: #fff2e6;
}

.warning-banner strong {
  display: block;
  margin-bottom: 8px;
}

.hero-panel,
.section-block,
.info-card,
.discover-card,
.match-card,
.modal-card {
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.88);
  border: 1px solid rgba(239, 208, 193, 0.72);
}

.hero-panel,
.section-block {
  margin-top: 22px;
  padding: 26px;
  box-shadow: 0 22px 44px rgba(109, 57, 41, 0.08);
}

.hero-panel {
  display: grid;
  grid-template-columns: 1.35fr 0.9fr;
  gap: 24px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 16px;
}

.status-active { background: rgba(179, 233, 201, 0.8); color: #176540; }
.status-pending { background: rgba(255, 225, 179, 0.85); color: #7a4e0b; }
.status-rejected { background: rgba(255, 204, 204, 0.88); color: #902d2d; }
.status-suspended { background: rgba(255, 220, 220, 0.95); color: #8c2f2f; }

.hero-copy p {
  color: #6d554d;
  line-height: 1.7;
}

.hero-highlights,
.discover-grid,
.match-grid,
.profile-layout-grid,
.safety-grid,
.action-row,
.chip-row,
.tag-row {
  display: grid;
  gap: 14px;
}

.hero-highlights {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 24px;
}

.highlight-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(235, 209, 199, 0.85);
}

.highlight-card span,
.match-badge,
.discover-meta,
.info-card dt {
  color: #916456;
  font-size: 13px;
  font-weight: 700;
}

.highlight-card strong {
  display: block;
  margin-top: 10px;
  font-size: 1.4rem;
}

.hero-visual {
  display: grid;
  gap: 16px;
}

.profile-frame {
  min-height: 420px;
  border-radius: 28px;
  overflow: hidden;
  background: linear-gradient(180deg, #f3c8b0 0%, #e89a79 100%);
}

.profile-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-placeholder {
  min-height: 420px;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 24px;
  color: #fffaf7;
}

.profile-placeholder span {
  width: 96px;
  height: 96px;
  display: inline-grid;
  place-items: center;
  margin-bottom: 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 2.4rem;
  font-weight: 800;
}

.mini-profile {
  padding: 18px 20px;
  border-radius: 24px;
  background: rgba(52, 33, 29, 0.92);
  color: #fff;
}

.mini-profile strong,
.mini-profile p {
  color: #fff;
}

.chip-row,
.tag-row {
  grid-auto-flow: column;
  grid-auto-columns: max-content;
  justify-content: start;
  flex-wrap: wrap;
}

.chip,
.tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.chip {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.tag {
  background: #f8ede7;
  color: #7d5a50;
}

.tag.soft {
  background: #fff6f1;
}

.section-head h3 {
  margin: 0;
  font-size: 1.8rem;
}

.discover-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.discover-card {
  overflow: hidden;
}

.discover-photo {
  min-height: 260px;
  padding: 16px;
  display: flex;
  align-items: flex-end;
}

.discover-overlay {
  width: 100%;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(52, 33, 29, 0.08) 0%, rgba(52, 33, 29, 0.82) 100%);
}

.discover-overlay span {
  display: inline-flex;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 244, 239, 0.18);
  color: #fff7f2;
  font-size: 12px;
  font-weight: 700;
}

.discover-overlay strong {
  display: block;
  margin-top: 12px;
  color: #fff;
  font-size: 1.5rem;
}

.discover-body,
.info-card,
.match-card {
  padding: 20px;
}

.discover-body p,
.match-card p {
  color: #634d45;
  line-height: 1.68;
}

.action-row {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.action-button.pass { background: #f3ebe5; color: #654b43; }
.action-button.detail { background: #fff3ec; color: #a6593e; }
.action-button.like { background: linear-gradient(135deg, #d56f4e 0%, #eea27d 100%); color: #fff; }

.match-grid,
.safety-grid {
  grid-template-columns: 1fr 1fr;
}

.queue-list,
.tip-list {
  margin: 16px 0 0;
  padding-left: 18px;
}

.queue-list {
  list-style: none;
  padding: 0;
}

.queue-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(235, 209, 199, 0.85);
}

.queue-list li:last-child {
  border-bottom: none;
}

.profile-layout-grid {
  grid-template-columns: 1fr 1.1fr 0.9fr;
}

.info-card dl {
  display: grid;
  gap: 14px;
  margin: 0;
}

.info-card dd {
  margin: 8px 0 0;
  color: #33241f;
  font-weight: 700;
}

.report-card label {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: #503731;
  font-weight: 600;
}

.report-card input,
.report-card select,
.report-card textarea {
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
}

.report-actions {
  margin-top: 16px;
}

.loading-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
}

.loading-card {
  width: min(480px, 100%);
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 251, 248, 0.88);
  border: 1px solid rgba(239, 208, 193, 0.72);
  text-align: center;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(34, 22, 18, 0.55);
  display: grid;
  place-items: center;
  padding: 20px;
  z-index: 20;
}

.modal-card {
  width: min(760px, 100%);
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(40, 24, 20, 0.28);
}

.modal-photo {
  min-height: 260px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  color: #fff;
}

.modal-photo span {
  display: inline-flex;
  width: fit-content;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.2);
  font-size: 12px;
  font-weight: 700;
}

.modal-photo strong {
  margin-top: 14px;
  color: #fff;
  font-size: 2rem;
}

.modal-body {
  padding: 24px;
}

.modal-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

@media (max-width: 1100px) {
  .hero-panel,
  .hero-highlights,
  .discover-grid,
  .match-grid,
  .profile-layout-grid,
  .safety-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .home-page {
    padding: 22px 14px 40px;
  }

  .topbar,
  .section-head,
  .topbar-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-row {
    grid-template-columns: 1fr;
  }

  .profile-frame,
  .profile-placeholder {
    min-height: 320px;
  }
}
</style>