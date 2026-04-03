<template>
  <div>로그인 처리중...</div>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

onMounted(() => {
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");

  if (token) {
    localStorage.setItem("token", token);
    router.push("/home");
  } else {
    alert("로그인 실패");
    router.push("/");
  }
});
</script>