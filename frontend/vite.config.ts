import { defineConfig, loadEnv, type Plugin } from "vite";
import react from "@vitejs/plugin-react";

/** Social cards need absolute image URLs, and the site's origin is only known per deploy. */
function siteUrl(url: string): Plugin {
  return {
    name: "site-url",
    transformIndexHtml: (html) => html.replaceAll("{{SITE_URL}}", url.replace(/\/$/, "")),
  };
}

// Vercel sets this at build time, so deploys there need no SITE_URL.
function vercelProductionUrl(env: Record<string, string>): string {
  const host = env.VERCEL_PROJECT_PRODUCTION_URL;
  return host ? `https://${host}` : "";
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_PROXY_TARGET || "http://localhost:8080";

  return {
    plugins: [react(), siteUrl(env.SITE_URL || vercelProductionUrl(env))],
    server: {
      port: 5173,
      proxy: {
        "/api": apiTarget,
      },
    },
  };
});
