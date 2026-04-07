import { createRouter, createWebHistory } from "vue-router";
import { hasToken } from "../utils/auth";
import AdminReviews from "../views/admin/AdminReviews.vue";
import ScamMonitor from "../views/admin/ScamMonitor.vue";
import Login from "../views/auth/Login.vue";
import ResetPassword from "../views/auth/ResetPassword.vue";
import Signup from "../views/auth/Signup.vue";
import SocialLogin from "../views/auth/SocialLogin.vue";
import Chats from "../views/user/Chats.vue";
import Home from "../views/user/Home.vue";
import Profile from "../views/user/Profile.vue";
import ReviewPending from "../views/user/ReviewPending.vue";
import SignupComplete from "../views/user/SignupComplete.vue";
import Today from "../views/user/Today.vue";
import UserLayout from "../views/user/UserLayout.vue";

const routes = [
  { path: "/", component: Login, meta: { guestOnly: true } },
  { path: "/admin/reviews", component: AdminReviews },
  { path: "/admin/safety", component: ScamMonitor },
  { path: "/signup", component: Signup, meta: { guestOnly: true } },
  { path: "/reset-password", component: ResetPassword, meta: { guestOnly: true } },
  { path: "/review-pending", component: ReviewPending, meta: { requiresAuth: true } },
  { path: "/signup-complete", component: SignupComplete, meta: { requiresAuth: true } },
  { path: "/social-login", component: SocialLogin },
  {
    path: "/app",
    component: UserLayout,
    meta: { requiresAuth: true, userApp: true },
    children: [
      { path: "", redirect: "/home" },
      { path: "/home", component: Home },
      { path: "/today", component: Today },
      { path: "/chats", component: Chats },
      { path: "/profile", component: Profile },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !hasToken()) {
    return "/";
  }

  if (to.meta.guestOnly && hasToken()) {
    return "/home";
  }

  return true;
});

export default router;
