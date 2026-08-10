import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const repositoryPagesBase = "/discount-coupons-management/";

export default defineConfig({
  base: process.env.GITHUB_PAGES === "true" ? repositoryPagesBase : "/",
  plugins: [react()]
});
