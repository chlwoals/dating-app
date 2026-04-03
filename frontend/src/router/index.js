import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/Login.vue";
import Home from "../views/Home.vue";
import SocialLogin from "../views/SocialLogin.vue";

const routes = [
  { path: "/", component: Login },
  { path: "/home", component: Home },
  { path: "/social-login", component: SocialLogin },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;