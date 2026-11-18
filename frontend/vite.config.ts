import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_PROXY_TARGET || "http://localhost:8080";
  const mlTarget = env.VITE_ML_PROXY_TARGET || "http://localhost:8000";

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        "/api": apiTarget,
        "/ml": {
          target: mlTarget,
          rewrite: (path) => path.replace(/^\/ml/, ""),
        },
      },
    },
  };
});
