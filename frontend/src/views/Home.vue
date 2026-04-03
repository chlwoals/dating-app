<template>
  <div>
    <h2>메인 페이지</h2>

    <div v-if="user">
      <p>이메일: {{ user.email }}</p>
      <p>닉네임: {{ user.nickname }}</p>
    </div>

    <button @click="logout">로그아웃</button>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import api from "../api/api";
import { useRouter } from "vue-router";

const router = useRouter();
const user = ref(null);

onMounted(async () => {
  try {
    const res = await api.get("/user/me");
    user.value = res.data;
  } catch (e) {
    alert("로그인 필요");
    router.push("/");
  }
});

const logout = () => {
  localStorage.removeItem("token");
  router.push("/");
};
</script>