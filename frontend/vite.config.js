import { defineConfig } from "vite";
export default defineConfig({
  server: {
    proxy: {
      "/api": "http://localhost:9977",
    },
  },
});
