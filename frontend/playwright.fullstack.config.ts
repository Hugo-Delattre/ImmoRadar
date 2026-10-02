import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './fullstack',
  workers: 1,
  timeout: 60000,
  expect: { timeout: 15000 },
  reporter: 'line',
  use: {
    baseURL: 'http://localhost:4300',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  webServer: [
    {
      command:
        'mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:file:fullstack-stress?mode=memory&cache=shared --spring.jpa.hibernate.ddl-auto=create-drop --server.port=8080"',
      cwd: '../backend',
      url: 'http://localhost:8080/api/deals',
      timeout: 180000,
      reuseExistingServer: false,
    },
    {
      command: 'npm run start -- --port 4300',
      url: 'http://localhost:4300',
      timeout: 180000,
      reuseExistingServer: false,
    },
  ],
});
