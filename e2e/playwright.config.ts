import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.APP_BASE_URL ?? 'http://localhost:4202',
    video: { mode: 'on', size: { width: 1280, height: 800 } },
    trace: 'off',
    screenshot: 'off',
    viewport: { width: 1280, height: 800 }
  },
  outputDir: '../.recordings/raw'
});
