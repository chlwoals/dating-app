import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    // Allow access from the local network so phone testing works too.
    host: true,
    port: 5173,
    headers: {
      // Firebase/Google 팝업 로그인이 부모 창과 상태를 주고받을 수 있게 허용한다.
      "Cross-Origin-Opener-Policy": "same-origin-allow-popups",
    },
  },
});
