<template>
  <button class="filter-trigger" @click="open">추천조건</button>
  <dialog ref="modal" class="social-dialog preference-dialog" @click="e => !busy && e.target === modal && modal.close()">
    <form @submit.prevent="save">
      <header><h2>추천조건</h2><button type="button" aria-label="닫기" :disabled="busy" @click="modal.close()">×</button></header>
      <p v-if="loading" role="status">추천조건을 불러오는 중…</p>
      <template v-if="ready">
        <p class="muted">변경한 조건은 {{ effective }}부터 적용돼요.</p>
        <fieldset :disabled="busy">
          <div class="age-fields">
            <label>출생연도부터<select v-model="form.birthYearMin" aria-label="시작 출생연도"><option :value="null">상관없음</option><option v-for="year in years" :key="year" :value="year">{{ year }}년생 ({{ referenceYear - year }}세)</option></select></label>
            <label>출생연도까지<select v-model="form.birthYearMax" aria-label="마지막 출생연도"><option :value="null">상관없음</option><option v-for="year in years" :key="year" :value="year">{{ year }}년생 ({{ referenceYear - year }}세)</option></select></label>
          </div>
          <p class="age-note muted">괄호 안 나이는 {{ referenceYear }}년 기준 연나이입니다.</p>
          <div class="radius-setting">
            <label for="recommendation-radius">지역범위 <strong>{{ radius === 505 ? '상관없음' : `반경 ${radius}km` }}</strong></label>
            <input id="recommendation-radius" v-model.number="radius" class="radius-slider" type="range" min="5" max="505" step="5" :aria-valuetext="radius === 505 ? '상관없음' : `반경 ${radius}킬로미터`" />
            <div class="radius-labels"><span>5km</span><span>250km</span><span>상관없음</span></div>
            <p class="muted">{{ region || '프로필의 등록 지역' }} 중심점 기준 거리예요.</p>
            <p v-if="!radiusAvailable" class="muted">반경을 지정하려면 프로필의 거주 지역을 먼저 선택해 주세요.</p>
            <p v-if="legacyRegion" class="muted">기존 조건은 {{ legacyRegion }}입니다. 저장하면 선택한 반경으로 바뀝니다.</p>
          </div>
          <label v-for="field in fields" :key="field.key">{{ field.label }}<select v-model="form[field.key]"><option value="ANY">상관없음</option><option v-for="o in field.options" :key="o[0]" :value="o[0]">{{ o[1] }}</option></select></label>
        </fieldset>
      </template>
      <p v-if="error" role="alert">{{ error }}</p>
      <button v-if="!ready && !loading" type="button" @click="load">다시 불러오기</button>
      <p v-if="saved" role="status">저장했어요. 현재 카드는 그대로 유지됩니다.</p>
      <button v-if="ready" class="primary" :disabled="busy">{{ busy ? '저장 중…' : '다음 회차에 적용' }}</button>
    </form>
  </dialog>
</template>
<script setup>
import { ref, computed } from 'vue';
import api from '../api/api';
import './social.css';
const modal=ref(),form=ref({}),at=ref(),busy=ref(false),ready=ref(false),loading=ref(false),error=ref(''),saved=ref(false);
const referenceYear=ref(2026),radius=ref(505),region=ref(''),radiusAvailable=ref(false),legacyRegion=ref('');
const years=computed(()=>Array.from({length:83},(_,i)=>referenceYear.value-18-i));
const effective=computed(()=>at.value?new Date(at.value).toLocaleString('ko-KR',{timeZone:'Asia/Seoul',month:'long',day:'numeric',hour:'numeric',minute:'2-digit'}):'다음 회차');
const fields=[{key:'mbti',label:'희망 MBTI',options:'INTJ INTP ENTJ ENTP INFJ INFP ENFJ ENFP ISTJ ISFJ ESTJ ESFJ ISTP ISFP ESTP ESFP'.split(' ').map(x=>[x,x])},{key:'smoking',label:'흡연여부',options:[['NON_SMOKER','비흡연'],['SMOKER','흡연'],['OCCASIONAL','가끔 흡연']]},{key:'drinking',label:'음주여부',options:[['NONE','안 마심'],['SOMETIMES','가끔'],['OFTEN','자주']]},{key:'religion',label:'종교',options:[['NONE','무교'],['CHRISTIAN','기독교'],['CATHOLIC','천주교'],['BUDDHIST','불교'],['OTHER','기타']]}];
function failure(e,fallback){
  if(e.response?.status===404)return '추천조건 API가 없습니다. 백엔드 서버를 최신 코드로 재시작해 주세요.';
  if(!e.response)return '서버에 연결하지 못했어요. 백엔드 실행 상태를 확인해 주세요.';
  return e.response.data?.message||fallback;
}
async function open(){if(!modal.value.open)modal.value.showModal();await load();}
async function load(){
  if(loading.value)return;
  loading.value=true;ready.value=false;error.value='';saved.value=false;
  try{
    const {data}=await api.get('/social/preferences');
    if(!Number.isInteger(data.referenceYear)){error.value='출생연도·거리 설정을 사용하려면 백엔드 서버를 최신 코드로 재시작해 주세요.';return;}
    referenceYear.value=data.referenceYear;region.value=data.region;radiusAvailable.value=data.radiusAvailable;
    const c=data.pending||data.active;
    form.value={...c,birthYearMin:c.birthYearMin ?? (c.ageMax==null?null:data.referenceYear-c.ageMax),birthYearMax:c.birthYearMax ?? (c.ageMin==null?null:data.referenceYear-c.ageMin)};
    radius.value=c.radiusKm??505;
    legacyRegion.value=c.radiusKm==null?({CITY:'같은 시·도',DISTRICT:'같은 시·군·구'}[c.regionScope]||''):'';
    at.value=data.effectiveAt||data.nextRoundAt;ready.value=true;
  }catch(e){error.value=failure(e,'추천조건을 불러오지 못했어요.');}finally{loading.value=false;}
}
async function save(){
  if(busy.value||!ready.value)return;error.value='';saved.value=false;
  if(form.value.birthYearMin!=null&&form.value.birthYearMax!=null&&form.value.birthYearMin>form.value.birthYearMax){error.value='시작 출생연도는 마지막 출생연도보다 앞선 연도를 선택해 주세요.';return;}
  if(radius.value!==505&&!radiusAvailable.value){error.value='프로필의 거주 지역을 먼저 선택해 주세요.';return;}
  busy.value=true;
  try{const {data}=await api.put('/social/preferences',{...form.value,ageMin:null,ageMax:null,regionScope:'ANY',radiusKm:radius.value===505?null:radius.value});at.value=data.effectiveAt;legacyRegion.value='';saved.value=true;}
  catch(e){error.value=failure(e,'추천조건을 저장하지 못했어요.');}finally{busy.value=false;}
}
</script>
<style scoped>
.preference-dialog fieldset{border:0;margin:0;padding:0;min-width:0}
.preference-dialog .age-fields{grid-template-columns:1fr;gap:0}
.age-note{margin-top:0}.radius-setting{margin:20px 0;padding:16px;background:#fff2ea;border-radius:16px}
.radius-setting label{display:flex;justify-content:space-between;align-items:center;margin:0 0 8px}
.radius-setting strong{color:#b75f42;font-size:15px}
.preference-dialog .radius-slider{appearance:auto!important;accent-color:#d56f4e;width:100%!important;height:44px;min-height:44px;padding:0!important;margin:0;border:0!important;background:transparent;cursor:pointer}
.radius-labels{display:flex;justify-content:space-between;font-size:11px;color:#947568}.radius-setting p{margin-bottom:0;font-size:11px}
</style>
