<template>
  <section class="admin-page">
    <div class="admin-card">
      <div class="page-head">
        <div>
          <p class="eyebrow">Admin Safety</p>
          <h1>로맨스 스캠 모니터</h1>
          <p class="sub-copy">프로필 위험 점수, 신고 내역, 운영 처리 현황을 한곳에서 확인합니다.</p>
        </div>
        <div class="nav-row">
          <RouterLink class="ghost-button" to="/admin/reviews">심사 관리로 이동</RouterLink>
        </div>
      </div>

      <div class="toolbar">
        <label>
          <span>운영자 키</span>
          <input v-model.trim="adminKey" type="password" placeholder="X-Admin-Key 입력" />
        </label>

        <label>
          <span>위험 상태</span>
          <select v-model="riskStatus">
            <option value="WATCH">주의</option>
            <option value="HIGH_RISK">고위험</option>
            <option value="ALL">전체</option>
          </select>
        </label>

        <label>
          <span>검색</span>
          <input v-model.trim="searchKeyword" type="text" placeholder="이메일, 닉네임, 지역, 직업" />
        </label>

        <label>
          <span>신고 상태</span>
          <select v-model="reportStatus">
            <option value="OPEN">처리 대기</option>
            <option value="RESOLVED">처리 완료</option>
          </select>
        </label>

        <button class="primary-button" @click="loadDashboard" :disabled="loading">
          {{ loading ? '불러오는 중...' : '대시보드 새로고침' }}
        </button>
      </div>

      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="summary-grid" v-if="summary">
        <article class="summary-card amber">
          <span>고위험 회원</span>
          <strong>{{ summary.highRiskUserCount }}명</strong>
          <p>즉시 점검이 필요한 회원 수입니다.</p>
        </article>
        <article class="summary-card soft">
          <span>주의 회원</span>
          <strong>{{ summary.watchUserCount }}명</strong>
          <p>위험 표현이 탐지된 회원 수입니다.</p>
        </article>
        <article class="summary-card sand">
          <span>열린 신고</span>
          <strong>{{ summary.openReportCount }}건</strong>
          <p>운영자 검토가 필요한 신고입니다.</p>
        </article>
        <article class="summary-card mint">
          <span>처리 완료</span>
          <strong>{{ summary.resolvedReportCount }}건</strong>
          <p>운영 처리 후 닫힌 신고입니다.</p>
        </article>
      </div>

      <div class="layout-grid">
        <section>
          <div class="section-head">
            <div>
              <h2>위험 회원 목록</h2>
              <p>프로필 문구 기준으로 감지된 주의/고위험 회원입니다.</p>
            </div>
          </div>

          <div v-if="riskUsers.length" class="risk-list">
            <article v-for="user in riskUsers" :key="user.userId" class="risk-card">
              <div class="risk-head">
                <div>
                  <strong>{{ user.nickname }}</strong>
                  <p>{{ user.email }}</p>
                </div>
                <div class="badge-group">
                  <span class="score-badge">점수 {{ user.fraudRiskScore }}</span>
                  <span class="status-badge" :class="user.fraudReviewStatus.toLowerCase()">{{ riskStatusLabel(user.fraudReviewStatus) }}</span>
                </div>
              </div>

              <div class="meta-grid">
                <div><span>상태</span><strong>{{ user.status }}</strong></div>
                <div><span>지역</span><strong>{{ user.region || '-' }}</strong></div>
                <div><span>직업</span><strong>{{ user.job || '-' }}</strong></div>
                <div><span>열린 신고</span><strong>{{ user.openReportCount }}건</strong></div>
              </div>

              <p class="risk-detail">최근 탐지: {{ user.latestRiskDetail }}</p>

              <div class="action-row">
                <button class="ghost-button" @click="toggleLogs(user.userId)">
                  {{ expandedUserId === user.userId ? '위험 로그 닫기' : '위험 로그 보기' }}
                </button>
              </div>

              <div v-if="expandedUserId === user.userId" class="logs-box">
                <p v-if="logsLoading" class="muted-copy">로그를 불러오는 중입니다.</p>
                <template v-else>
                  <article v-for="log in riskLogs[user.userId] || []" :key="log.id" class="log-item">
                    <div class="log-head">
                      <strong>{{ log.riskType }}</strong>
                      <span>{{ formatDateTime(log.createdAt) }}</span>
                    </div>
                    <p>{{ log.detail }}</p>
                    <small>점수 {{ log.score }}</small>
                  </article>
                  <p v-if="!(riskLogs[user.userId] || []).length" class="muted-copy">표시할 위험 로그가 없습니다.</p>
                </template>
              </div>
            </article>
          </div>

          <p v-else class="empty-state">조건에 맞는 위험 회원이 없습니다.</p>
        </section>

        <section>
          <div class="section-head">
            <div>
              <h2>사용자 신고 내역</h2>
              <p>의심 계정 신고를 검토하고 처리 완료로 전환할 수 있습니다.</p>
            </div>
          </div>

          <div v-if="reports.length" class="report-list">
            <article v-for="report in reports" :key="report.id" class="report-card">
              <div class="report-head">
                <strong>#{{ report.id }} · {{ reasonLabel(report.reasonCode) }}</strong>
                <span class="report-status">{{ report.status }}</span>
              </div>
              <p class="report-meta">신고자 {{ report.reporterUserId }} → 대상 {{ report.reportedUserId }}</p>
              <p class="report-detail">{{ report.detail }}</p>
              <p class="report-meta">접수 시각 {{ formatDateTime(report.createdAt) }}</p>

              <label class="resolve-box">
                <span>운영 처리 메모</span>
                <textarea
                  v-model.trim="resolveNotes[report.id]"
                  rows="3"
                  placeholder="처리 결과나 추후 조치를 기록해주세요."
                ></textarea>
              </label>

              <div class="action-row">
                <button class="primary-button compact" @click="resolveReport(report.id)" :disabled="report.status === 'RESOLVED'">
                  처리 완료로 변경
                </button>
              </div>
            </article>
          </div>

          <p v-else class="empty-state">조건에 맞는 신고가 없습니다.</p>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import axios from "axios";

const hostname = window.location.hostname || "localhost";
const baseURL = `http://${hostname}:8080/api`;

const adminKey = ref(localStorage.getItem("adminReviewKey") || "");
const riskStatus = ref("WATCH");
const reportStatus = ref("OPEN");
const searchKeyword = ref("");
const summary = ref(null);
const riskUsers = ref([]);
const reports = ref([]);
const riskLogs = ref({});
const resolveNotes = ref({});
const expandedUserId = ref(null);
const logsLoading = ref(false);
const loading = ref(false);
const message = ref("");
const errorMessage = ref("");

const getHeaders = () => ({
  "X-Admin-Key": adminKey.value,
});

const persistAdminKey = () => {
  localStorage.setItem("adminReviewKey", adminKey.value);
};

const loadDashboard = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    persistAdminKey();
    const [summaryRes, usersRes, reportsRes] = await Promise.all([
      axios.get(`${baseURL}/admin/safety/summary`, { headers: getHeaders() }),
      axios.get(`${baseURL}/admin/safety/users`, {
        headers: getHeaders(),
        params: { riskStatus: riskStatus.value, q: searchKeyword.value },
      }),
      axios.get(`${baseURL}/admin/safety/reports`, {
        headers: getHeaders(),
        params: { status: reportStatus.value },
      }),
    ]);

    summary.value = summaryRes.data;
    riskUsers.value = usersRes.data;
    reports.value = reportsRes.data;
    resolveNotes.value = Object.fromEntries(reports.value.map((report) => [report.id, report.adminNote || ""]));
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "스캠 모니터 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const toggleLogs = async (userId) => {
  if (expandedUserId.value === userId) {
    expandedUserId.value = null;
    return;
  }

  expandedUserId.value = userId;
  if (riskLogs.value[userId]) {
    return;
  }

  logsLoading.value = true;
  try {
    const { data } = await axios.get(`${baseURL}/admin/safety/users/${userId}/logs`, {
      headers: getHeaders(),
    });
    riskLogs.value = {
      ...riskLogs.value,
      [userId]: data,
    };
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "위험 로그를 불러오지 못했습니다.";
  } finally {
    logsLoading.value = false;
  }
};

const resolveReport = async (reportId) => {
  const adminNote = resolveNotes.value[reportId];
  if (!adminNote) {
    alert("처리 메모를 먼저 입력해주세요.");
    return;
  }

  try {
    const { data } = await axios.put(
      `${baseURL}/admin/safety/reports/${reportId}/resolve`,
      { adminNote },
      { headers: getHeaders() }
    );
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "신고 처리 상태를 변경하지 못했습니다.";
  }
};

const reasonLabel = (value) => {
  const labels = {
    INVESTMENT: "투자·금전 유도",
    EXTERNAL_CONTACT: "외부 메신저 유도",
    IMPERSONATION: "사칭 의심",
    HARASSMENT: "부적절한 접근",
    OTHER: "기타",
  };
  return labels[value] || value;
};

const riskStatusLabel = (value) => {
  const labels = {
    WATCH: "주의",
    HIGH_RISK: "고위험",
    NORMAL: "정상",
  };
  return labels[value] || value;
};

const formatDateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

onMounted(loadDashboard);
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #fff5ee 0%, #ffe2d2 100%);
}

.admin-card {
  width: min(100%, 1240px);
  margin: 0 auto;
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 251, 247, 0.97);
  box-shadow: 0 22px 56px rgba(92, 54, 40, 0.14);
}

.page-head,
.section-head,
.risk-head,
.report-head,
.log-head,
.action-row,
.nav-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #ab5331;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #35231c;
}

.sub-copy,
.section-head p,
.empty-state,
.muted-copy,
.report-meta,
.risk-detail,
.log-item p,
.log-item small {
  color: #6c534a;
}

.toolbar {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 1.25fr 0.8fr 1fr 0.9fr auto;
  gap: 16px;
  align-items: end;
}

label {
  display: grid;
  gap: 8px;
  color: #543932;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #e5c2b2;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.primary-button,
.ghost-button {
  border-radius: 14px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
}

.primary-button {
  border: none;
  background: #cd6d2d;
  color: #fff;
}

.ghost-button {
  border: 1px solid #e2bba9;
  background: #fff;
  color: #8a4d35;
}

.compact {
  padding: 11px 15px;
}

.message {
  margin-top: 16px;
}

.success {
  color: #1f7a43;
}

.error {
  color: #b72f2f;
}

.summary-grid,
.layout-grid,
.meta-grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  margin-top: 20px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.layout-grid {
  margin-top: 28px;
  grid-template-columns: 1.15fr 0.95fr;
  align-items: start;
}

.summary-card,
.risk-card,
.report-card,
.logs-box {
  border-radius: 20px;
  border: 1px solid #eed6cb;
}

.summary-card {
  padding: 18px;
}

.summary-card span {
  display: block;
  color: #8f6658;
  font-size: 13px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  color: #34211d;
  font-size: 1.7rem;
}

.summary-card p {
  margin: 10px 0 0;
  line-height: 1.55;
}

.amber { background: linear-gradient(180deg, rgba(255,239,229,0.98) 0%, rgba(255,223,205,0.96) 100%); }
.soft { background: linear-gradient(180deg, rgba(255,245,238,0.95) 0%, rgba(255,234,225,0.96) 100%); }
.sand { background: linear-gradient(180deg, rgba(255,249,240,0.95) 0%, rgba(247,230,214,0.96) 100%); }
.mint { background: linear-gradient(180deg, rgba(242,252,245,0.97) 0%, rgba(228,244,232,0.96) 100%); }

.risk-list,
.report-list {
  margin-top: 16px;
  display: grid;
  gap: 16px;
}

.risk-card,
.report-card {
  padding: 18px;
  background: #fff;
}

.risk-head p {
  margin: 6px 0 0;
}

.badge-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.score-badge,
.status-badge,
.report-status {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.score-badge {
  background: #fff3e9;
  color: #914f30;
}

.status-badge {
  background: #fff7ef;
  color: #855039;
}

.status-badge.watch {
  background: #fff0dc;
  color: #9b5f11;
}

.status-badge.high_risk {
  background: #ffe8e2;
  color: #ad3e2e;
}

.report-status {
  background: #fff3eb;
  color: #8c4d34;
}

.meta-grid {
  margin-top: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-grid span {
  display: block;
  margin-bottom: 6px;
  color: #916658;
  font-size: 13px;
}

.meta-grid strong {
  color: #38251f;
}

.risk-detail,
.report-detail {
  margin-top: 14px;
  line-height: 1.6;
}

.logs-box {
  margin-top: 14px;
  padding: 14px;
  background: #fff8f3;
}

.log-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #f0ddd3;
}

.log-item + .log-item {
  margin-top: 10px;
}

.resolve-box {
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .toolbar,
  .summary-grid,
  .layout-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .page-head,
  .risk-head,
  .report-head,
  .log-head,
  .action-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
