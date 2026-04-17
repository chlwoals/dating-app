import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    // Allow access from the local network so phone testing works too.
    host: true,
    port: 5173,
  },
});
