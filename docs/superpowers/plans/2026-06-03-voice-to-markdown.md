# 语音转 Markdown 概括系统（VoiceNotes）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个本地运行的 Web 应用，用户拖入会议/课程录音，系统转写为文字、用大模型流式输出 Markdown 概括，并可将概括 TTS 朗读成音频；支持历史记录、用户登录、多模板。

**Architecture:** 后端 Spring Boot（Spring MVC 分层 Controller→Service→Repository）+ MySQL（JPA）+ Spring Security/JWT。AI 能力：DashScope 实时识别 SDK 喂本地音频文件做转写（ASR），Qwen 流式接口做概括（经 `SseEmitter` 推送），speech-2.8-hd（OpenAI 兼容 TTS，经中转站）做朗读。前端 React + Vite + Tailwind + shadcn/ui + Framer Motion，通过 fetch/SSE/EventSource 与后端通信。

**Tech Stack:** Java 17, Spring Boot 3.x, Gradle, MySQL 8, Spring Data JPA, Spring Security, jjwt, dashscope-sdk-java, OkHttp（TTS HTTP）, JUnit 5 + Mockito + H2（测试）; React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui, Framer Motion, react-markdown, Vitest + React Testing Library。

---

## 文件结构

### 后端 `backend/`

```
backend/
├── build.gradle                         Gradle 构建与依赖
├── settings.gradle
├── src/main/resources/
│   └── application.yml                  配置（DB、JWT、DashScope、TTS）
├── src/main/java/com/voicenotes/
│   ├── VoiceNotesApplication.java       启动类
│   ├── config/
│   │   ├── SecurityConfig.java          Spring Security + JWT 过滤器链
│   │   ├── WebConfig.java               CORS、静态资源
│   │   └── AppProperties.java           DashScope/TTS/存储 配置绑定
│   ├── domain/
│   │   ├── User.java                    用户实体
│   │   ├── Transcription.java           核心实体
│   │   └── TranscriptionStatus.java     状态枚举
│   │   └── SummaryTemplate.java         模板枚举
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── TranscriptionRepository.java
│   ├── dto/
│   │   ├── AuthDtos.java                注册/登录请求与响应
│   │   ├── TranscriptionDtos.java       列表/详情 DTO
│   │   └── SpeechDtos.java              TTS 响应 DTO
│   ├── security/
│   │   ├── JwtService.java              JWT 签发/校验
│   │   ├── JwtAuthFilter.java           请求过滤器
│   │   └── AppUserDetailsService.java   UserDetailsService 实现
│   ├── service/
│   │   ├── AuthService.java             注册/登录
│   │   ├── TranscriptionService.java    状态流转、业务编排、越权校验
│   │   ├── DashScopeService.java        ASR（实时识别喂本地文件）+ Qwen 流式
│   │   ├── PromptTemplateService.java   按模板拼 Qwen prompt
│   │   ├── TtsService.java              speech-2.8-hd 调用、文本切分/音频拼接
│   │   └── StorageService.java          音频文件存取、删除
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── TranscriptionController.java 上传/转写/查询/列表/下载/删除
│   │   └── SummaryController.java       SSE 概括流、TTS 生成/播放
│   └── exception/
│       ├── ApiException.java            业务异常
│       └── GlobalExceptionHandler.java  统一错误响应
└── src/test/java/com/voicenotes/        测试镜像上述包
```

### 前端 `frontend/`

```
frontend/
├── package.json
├── vite.config.ts
├── tailwind.config.js                   设计 token（配色、字体）
├── index.html
├── src/
│   ├── main.tsx                         入口
│   ├── App.tsx                          路由（登录 / 工作台）
│   ├── lib/
│   │   ├── api.ts                       fetch 封装（带 JWT）
│   │   ├── sse.ts                       SSE 概括流接收
│   │   └── utils.ts                     cn() 等工具
│   ├── store/
│   │   └── auth.ts                      token 存取、登录态
│   ├── components/ui/                   shadcn 组件（button/card/progress/dialog/sonner…）
│   ├── components/
│   │   ├── Dropzone.tsx                 拖拽上传 + 校验 + 动效
│   │   ├── HistorySidebar.tsx           历史列表 + stagger 动效
│   │   ├── TranscribeProgress.tsx       转写进度 + 骨架屏 shimmer
│   │   ├── SummaryView.tsx              流式 Markdown 渲染 + 光标
│   │   ├── AudioPlayer.tsx              TTS 音频播放器
│   │   ├── Toolbar.tsx                  下载/复制/朗读 按钮组
│   │   ├── TemplateSelect.tsx           模板下拉
│   │   └── ThemeToggle.tsx              亮/暗色切换
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   └── WorkbenchPage.tsx            主工作台（编排各组件 + 状态机）
│   └── types.ts                         共享类型（与后端 DTO 对齐）
└── src/__tests__/                       Vitest 组件测试
```

---

## 阶段总览（里程碑）

- **M0 脚手架**：Task 1–2，后端能启动、前端能跑空页。
- **M1 数据与鉴权**：Task 3–6，实体/仓库、JWT 注册登录。
- **M2 上传与存储**：Task 7–8，音频落盘、建记录。
- **M3 转写**：Task 9–11，DashScope 实时识别喂本地文件，状态流转。
- **M4 概括流式**：Task 12–14，Qwen 流式 + SSE，模板拼接。
- **M5 朗读**：Task 15–16，TTS 生成音频、切分拼接、播放/下载接口。
- **M6 历史/下载/删除**：Task 17。
- **M7 前端基础**：Task 18–20，设计 token、API 封装、登录页。
- **M8 前端工作台**：Task 21–26，拖拽、进度、流式渲染、播放器、历史。
- **M9 动效与收尾**：Task 27–28。

每个 Task 末尾都 commit。后端用 `./gradlew test` 跑测试，前端用 `npm run test`。

> **环境前置**：本地需装 JDK 17、Node 18+、MySQL 8（或开发期用 H2）。运行后端前设环境变量 `DASHSCOPE_API_KEY`、`TTS_BASE_URL`、`TTS_API_KEY`、`JWT_SECRET`、`DB_USER`、`DB_PASSWORD`。密钥绝不写进代码或提交到 git。

---

### Task 1: 后端 Gradle 脚手架与启动类

**Files:**
- Create: `backend/settings.gradle`, `backend/build.gradle`, `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/voicenotes/VoiceNotesApplication.java`
- Create: `backend/src/test/resources/application-test.yml`
- Test: `backend/src/test/java/com/voicenotes/VoiceNotesApplicationTests.java`

- [ ] **Step 1: 写 settings.gradle 与 build.gradle**

`backend/settings.gradle`:

```gradle
rootProject.name = 'voicenotes'
```

`backend/build.gradle`:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.4'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.voicenotes'
version = '0.0.1'
java { sourceCompatibility = '17' }

repositories { mavenCentral() }

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    runtimeOnly 'com.mysql:mysql-connector-j'

    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    implementation 'com.alibaba:dashscope-sdk-java:2.18.2'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'com.h2database:h2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform() }
```

- [ ] **Step 2: 写 application.yml**

`backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/voicenotes?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate.format_sql: true
  servlet:
    multipart:
      max-file-size: 200MB
      max-request-size: 210MB

app:
  jwt:
    secret: ${JWT_SECRET:change-this-dev-secret-to-a-long-random-string-min-32-bytes}
    expiration-ms: 86400000
  storage:
    audio-dir: ${AUDIO_DIR:./data/audio}
    tts-dir: ${TTS_DIR:./data/tts}
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:}
    asr-model: paraformer-realtime-v2
    llm-model: qwen-plus
  tts:
    base-url: ${TTS_BASE_URL:}
    api-key: ${TTS_API_KEY:}
    model: speech-2.8-hd
    voice: ${TTS_VOICE:alloy}
    max-chars: 4000
```

- [ ] **Step 3: 写启动类**

`backend/src/main/java/com/voicenotes/VoiceNotesApplication.java`:

```java
package com.voicenotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VoiceNotesApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoiceNotesApplication.class, args);
    }
}
```

- [ ] **Step 4: 写测试配置与冒烟测试**

`backend/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ''
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
app:
  jwt:
    secret: test-secret-test-secret-test-secret-test-secret-1234
    expiration-ms: 3600000
  storage:
    audio-dir: ./build/test-audio
    tts-dir: ./build/test-tts
  dashscope:
    api-key: test-key
  tts:
    base-url: http://localhost:1
    api-key: test-key
```

`backend/src/test/java/com/voicenotes/VoiceNotesApplicationTests.java`:

```java
package com.voicenotes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VoiceNotesApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd backend && ./gradlew test --tests VoiceNotesApplicationTests`
Expected: BUILD SUCCESSFUL（注：后续 Task 才加 SecurityConfig 等 bean，此处仅验证 Spring 上下文能起）

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "chore: scaffold Spring Boot backend with gradle and config"
```

---

### Task 2: 前端 Vite 脚手架与设计 token

**Files:**
- Create: `frontend/package.json`, `vite.config.ts`, `vitest.config.ts`, `tsconfig.json`, `postcss.config.js`, `tailwind.config.js`, `index.html`
- Create: `frontend/src/main.tsx`, `App.tsx`, `index.css`, `lib/utils.ts`
- Test: `frontend/src/__tests__/setup.ts`, `frontend/src/__tests__/smoke.test.tsx`

- [ ] **Step 1: 写 package.json**

`frontend/package.json`:

```json
{
  "name": "voicenotes-frontend",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview",
    "test": "vitest run"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.26.2",
    "react-markdown": "^9.0.1",
    "remark-gfm": "^4.0.0",
    "framer-motion": "^11.5.4",
    "lucide-react": "^0.445.0",
    "sonner": "^1.5.0",
    "clsx": "^2.1.1",
    "tailwind-merge": "^2.5.2"
  },
  "devDependencies": {
    "@types/react": "^18.3.5",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.1",
    "typescript": "^5.5.4",
    "vite": "^5.4.6",
    "tailwindcss": "^3.4.11",
    "postcss": "^8.4.47",
    "autoprefixer": "^10.4.20",
    "vitest": "^2.1.1",
    "jsdom": "^25.0.0",
    "@testing-library/react": "^16.0.1",
    "@testing-library/jest-dom": "^6.5.0",
    "@testing-library/user-event": "^14.5.2"
  }
}
```

- [ ] **Step 2: 写配置文件**

`frontend/vite.config.ts`:

```ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } },
});
```

`frontend/vitest.config.ts`:

```ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  test: { environment: 'jsdom', globals: true, setupFiles: './src/__tests__/setup.ts' },
});
```

`frontend/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "jsx": "react-jsx",
    "strict": true,
    "types": ["vitest/globals", "@testing-library/jest-dom"],
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] }
  },
  "include": ["src"]
}
```

`frontend/postcss.config.js`:

```js
export default { plugins: { tailwindcss: {}, autoprefixer: {} } };
```

`frontend/tailwind.config.js`:

```js
/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#0D9488', fg: '#FFFFFF' },
        accent: { DEFAULT: '#EA580C', fg: '#FFFFFF' },
        background: '#F0FDFA',
        foreground: '#134E4A',
        muted: '#E8F1F4',
        bordercolor: '#99F6E4',
        destructive: '#DC2626',
      },
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'] },
    },
  },
  plugins: [],
};
```

- [ ] **Step 3: 写入口与样式**

`frontend/index.html`:

```html
<!doctype html>
<html lang="zh">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet" />
    <title>VoiceNotes</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

`frontend/src/index.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

body { @apply bg-background text-foreground font-sans antialiased; }
.dark body { @apply bg-[#0b1f1d] text-[#d6f5ef]; }
```

`frontend/src/lib/utils.ts`:

```ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

`frontend/src/main.tsx`:

```tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
```

`frontend/src/App.tsx`:

```tsx
export default function App() {
  return <div className="p-8 text-2xl font-semibold">VoiceNotes</div>;
}
```

- [ ] **Step 4: 写测试**

`frontend/src/__tests__/setup.ts`:

```ts
import '@testing-library/jest-dom';
```

`frontend/src/__tests__/smoke.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import App from '../App';

test('renders app title', () => {
  render(<App />);
  expect(screen.getByText('VoiceNotes')).toBeInTheDocument();
});
```

- [ ] **Step 5: 安装依赖并测试**

Run: `cd frontend && npm install && npm run test`
Expected: 1 passed

- [ ] **Step 6: Commit**

```bash
git add frontend/
git commit -m "chore: scaffold React+Vite frontend with tailwind design tokens"
```

### Task 3: 领域实体与枚举

**Files:**
- Create: `backend/src/main/java/com/voicenotes/domain/TranscriptionStatus.java`
- Create: `backend/src/main/java/com/voicenotes/domain/SummaryTemplate.java`
- Create: `backend/src/main/java/com/voicenotes/domain/User.java`
- Create: `backend/src/main/java/com/voicenotes/domain/Transcription.java`
- Test: `backend/src/test/java/com/voicenotes/domain/TranscriptionTest.java`

- [ ] **Step 1: 写枚举**

`backend/src/main/java/com/voicenotes/domain/TranscriptionStatus.java`:

```java
package com.voicenotes.domain;

public enum TranscriptionStatus {
    UPLOADED, TRANSCRIBING, TRANSCRIBED, SUMMARIZING, DONE, FAILED
}
```

`backend/src/main/java/com/voicenotes/domain/SummaryTemplate.java`:

```java
package com.voicenotes.domain;

public enum SummaryTemplate {
    MEETING, LECTURE, GENERAL
}
```

- [ ] **Step 2: 写 User 实体**

`backend/src/main/java/com/voicenotes/domain/User.java`:

```java
package com.voicenotes.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: 写 Transcription 实体**

`backend/src/main/java/com/voicenotes/domain/Transcription.java`:

```java
package com.voicenotes.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "transcriptions")
public class Transcription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String audioPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TranscriptionStatus status = TranscriptionStatus.UPLOADED;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String transcriptText;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String summaryMarkdown;

    private String ttsAudioPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryTemplate template = SummaryTemplate.GENERAL;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String v) { this.originalFilename = v; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String v) { this.audioPath = v; }
    public TranscriptionStatus getStatus() { return status; }
    public void setStatus(TranscriptionStatus v) { this.status = v; }
    public String getTranscriptText() { return transcriptText; }
    public void setTranscriptText(String v) { this.transcriptText = v; }
    public String getSummaryMarkdown() { return summaryMarkdown; }
    public void setSummaryMarkdown(String v) { this.summaryMarkdown = v; }
    public String getTtsAudioPath() { return ttsAudioPath; }
    public void setTtsAudioPath(String v) { this.ttsAudioPath = v; }
    public SummaryTemplate getTemplate() { return template; }
    public void setTemplate(SummaryTemplate v) { this.template = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
```

- [ ] **Step 4: 写实体测试（验证默认值）**

`backend/src/test/java/com/voicenotes/domain/TranscriptionTest.java`:

```java
package com.voicenotes.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TranscriptionTest {
    @Test
    void defaultsAreSet() {
        Transcription t = new Transcription();
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.UPLOADED);
        assertThat(t.getTemplate()).isEqualTo(SummaryTemplate.GENERAL);
        assertThat(t.getCreatedAt()).isNotNull();
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd backend && ./gradlew test --tests TranscriptionTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/voicenotes/domain backend/src/test/java/com/voicenotes/domain
git commit -m "feat: add User and Transcription domain entities"
```

---

### Task 4: 仓库层与 AppProperties 配置绑定

**Files:**
- Create: `backend/src/main/java/com/voicenotes/repository/UserRepository.java`
- Create: `backend/src/main/java/com/voicenotes/repository/TranscriptionRepository.java`
- Create: `backend/src/main/java/com/voicenotes/config/AppProperties.java`
- Test: `backend/src/test/java/com/voicenotes/repository/TranscriptionRepositoryTest.java`

- [ ] **Step 1: 写 UserRepository**

`backend/src/main/java/com/voicenotes/repository/UserRepository.java`:

```java
package com.voicenotes.repository;

import com.voicenotes.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 2: 写 TranscriptionRepository**

`backend/src/main/java/com/voicenotes/repository/TranscriptionRepository.java`:

```java
package com.voicenotes.repository;

import com.voicenotes.domain.Transcription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TranscriptionRepository extends JpaRepository<Transcription, Long> {
    List<Transcription> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Transcription> findByIdAndUserId(Long id, Long userId);
}
```

- [ ] **Step 3: 写 AppProperties（绑定 application.yml 的 app.* ）**

`backend/src/main/java/com/voicenotes/config/AppProperties.java`:

```java
package com.voicenotes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private DashScope dashscope = new DashScope();
    private Tts tts = new Tts();

    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000;
        public String getSecret() { return secret; }
        public void setSecret(String v) { this.secret = v; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long v) { this.expirationMs = v; }
    }
    public static class Storage {
        private String audioDir;
        private String ttsDir;
        public String getAudioDir() { return audioDir; }
        public void setAudioDir(String v) { this.audioDir = v; }
        public String getTtsDir() { return ttsDir; }
        public void setTtsDir(String v) { this.ttsDir = v; }
    }
    public static class DashScope {
        private String apiKey;
        private String asrModel = "paraformer-realtime-v2";
        private String llmModel = "qwen-plus";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getAsrModel() { return asrModel; }
        public void setAsrModel(String v) { this.asrModel = v; }
        public String getLlmModel() { return llmModel; }
        public void setLlmModel(String v) { this.llmModel = v; }
    }
    public static class Tts {
        private String baseUrl;
        private String apiKey;
        private String model = "speech-2.8-hd";
        private String voice = "alloy";
        private int maxChars = 4000;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getModel() { return model; }
        public void setModel(String v) { this.model = v; }
        public String getVoice() { return voice; }
        public void setVoice(String v) { this.voice = v; }
        public int getMaxChars() { return maxChars; }
        public void setMaxChars(int v) { this.maxChars = v; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt v) { this.jwt = v; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage v) { this.storage = v; }
    public DashScope getDashscope() { return dashscope; }
    public void setDashscope(DashScope v) { this.dashscope = v; }
    public Tts getTts() { return tts; }
    public void setTts(Tts v) { this.tts = v; }
}
```

- [ ] **Step 2 之外测试：写仓库测试（@DataJpaTest + H2）**

`backend/src/test/java/com/voicenotes/repository/TranscriptionRepositoryTest.java`:

```java
package com.voicenotes.repository;

import com.voicenotes.domain.Transcription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TranscriptionRepositoryTest {
    @Autowired TranscriptionRepository repo;

    @Test
    void findsByUserIdOrdered() {
        Transcription a = make(1L, "a.mp3");
        Transcription b = make(1L, "b.mp3");
        Transcription other = make(2L, "c.mp3");
        repo.saveAll(List.of(a, b, other));

        List<Transcription> mine = repo.findByUserIdOrderByCreatedAtDesc(1L);
        assertThat(mine).hasSize(2);

        assertThat(repo.findByIdAndUserId(other.getId(), 1L)).isEmpty();
        assertThat(repo.findByIdAndUserId(other.getId(), 2L)).isPresent();
    }

    private Transcription make(Long userId, String name) {
        Transcription t = new Transcription();
        t.setUserId(userId);
        t.setOriginalFilename(name);
        t.setAudioPath("/tmp/" + name);
        return t;
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `cd backend && ./gradlew test --tests TranscriptionRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voicenotes/repository backend/src/main/java/com/voicenotes/config/AppProperties.java backend/src/test/java/com/voicenotes/repository
git commit -m "feat: add JPA repositories and app config properties"
```

---

### Task 5: JWT 服务、过滤器与安全配置

**Files:**
- Create: `backend/src/main/java/com/voicenotes/security/JwtService.java`
- Create: `backend/src/main/java/com/voicenotes/security/AppUserDetailsService.java`
- Create: `backend/src/main/java/com/voicenotes/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/voicenotes/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/voicenotes/config/WebConfig.java`
- Test: `backend/src/test/java/com/voicenotes/security/JwtServiceTest.java`

- [ ] **Step 1: 写 JwtService 测试（先红）**

`backend/src/test/java/com/voicenotes/security/JwtServiceTest.java`:

```java
package com.voicenotes.security;

import com.voicenotes.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    JwtService jwt;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-secret-test-secret-test-secret-test-secret-1234");
        props.getJwt().setExpirationMs(3600000);
        jwt = new JwtService(props);
    }

    @Test
    void roundTripSubject() {
        String token = jwt.generateToken("alice", 42L);
        assertThat(jwt.extractUsername(token)).isEqualTo("alice");
        assertThat(jwt.extractUserId(token)).isEqualTo(42L);
        assertThat(jwt.isValid(token, "alice")).isTrue();
    }

    @Test
    void invalidTokenRejected() {
        assertThat(jwt.isValid("garbage.token.value", "alice")).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && ./gradlew test --tests JwtServiceTest`
Expected: FAIL（JwtService 类不存在，编译错误）

- [ ] **Step 3: 实现 JwtService**

`backend/src/main/java/com/voicenotes/security/JwtService.java`:

```java
package com.voicenotes.security;

import com.voicenotes.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = props.getJwt().getExpirationMs();
    }

    public String generateToken(String username, Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public Long extractUserId(String token) {
        return parse(token).get("uid", Long.class);
    }

    public boolean isValid(String token, String username) {
        try {
            Claims c = parse(token);
            return c.getSubject().equals(username) && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && ./gradlew test --tests JwtServiceTest`
Expected: PASS

- [ ] **Step 5: 写 UserDetailsService、JwtAuthFilter、SecurityConfig、WebConfig**

`backend/src/main/java/com/voicenotes/security/AppUserDetailsService.java`:

```java
package com.voicenotes.security;

import com.voicenotes.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public AppUserDetailsService(UserRepository users) { this.users = users; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        return new User(u.getUsername(), u.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
```

`backend/src/main/java/com/voicenotes/security/JwtAuthFilter.java`:

```java
package com.voicenotes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final AppUserDetailsService uds;

    public JwtAuthFilter(JwtService jwt, AppUserDetailsService uds) {
        this.jwt = jwt;
        this.uds = uds;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req,
                                    @NonNull HttpServletResponse res,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            // SSE/EventSource 无法设 header，允许 ?token= 传递
            String q = req.getParameter("token");
            if (q != null && !q.isBlank()) token = q;
        }

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwt.extractUsername(token);
                if (jwt.isValid(token, username)) {
                    UserDetails ud = uds.loadUserByUsername(username);
                    var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // 无效 token：保持未认证，后续由 security 返回 401
            }
        }
        chain.doFilter(req, res);
    }
}
```

`backend/src/main/java/com/voicenotes/config/SecurityConfig.java`:

```java
package com.voicenotes.config;

import com.voicenotes.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;
    public SecurityConfig(JwtAuthFilter jwtFilter) { this.jwtFilter = jwtFilter; }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    AuthenticationEntryPoint entryPoint() {
        return (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint()))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

`backend/src/main/java/com/voicenotes/config/WebConfig.java`:

```java
package com.voicenotes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

- [ ] **Step 6: 运行全部测试**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL（contextLoads 现在加载了 Security 链）

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/voicenotes/security backend/src/main/java/com/voicenotes/config
git commit -m "feat: add JWT security with auth filter and config"
```

### Task 6: 认证服务、DTO、全局异常与 AuthController

**Files:**
- Create: `backend/src/main/java/com/voicenotes/dto/AuthDtos.java`
- Create: `backend/src/main/java/com/voicenotes/exception/ApiException.java`
- Create: `backend/src/main/java/com/voicenotes/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/voicenotes/service/AuthService.java`
- Create: `backend/src/main/java/com/voicenotes/controller/AuthController.java`
- Test: `backend/src/test/java/com/voicenotes/service/AuthServiceTest.java`
- Test: `backend/src/test/java/com/voicenotes/controller/AuthControllerIT.java`

- [ ] **Step 1: 写 DTO 与异常类**

`backend/src/main/java/com/voicenotes/dto/AuthDtos.java`:

```java
package com.voicenotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 32) String username,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {}

    public record AuthResponse(String token, String username) {}
}
```

`backend/src/main/java/com/voicenotes/exception/ApiException.java`:

```java
package com.voicenotes.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
```

`backend/src/main/java/com/voicenotes/exception/GlobalExceptionHandler.java`:

```java
package com.voicenotes.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("validation error");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage() == null ? "server error" : ex.getMessage()));
    }
}
```

- [ ] **Step 2: 写 AuthService 测试（先红）**

`backend/src/test/java/com/voicenotes/service/AuthServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.User;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.UserRepository;
import com.voicenotes.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    UserRepository users;
    PasswordEncoder encoder;
    JwtService jwt;
    AuthService service;

    @BeforeEach
    void setup() {
        users = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        jwt = mock(JwtService.class);
        when(jwt.generateToken(anyString(), any())).thenReturn("tok");
        service = new AuthService(users, encoder, jwt);
    }

    @Test
    void registerRejectsDuplicate() {
        when(users.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> service.register("alice", "secret123"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void registerHashesPassword() {
        when(users.existsByUsername("bob")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0); u.setId(1L); return u;
        });
        var resp = service.register("bob", "secret123");
        assertThat(resp.token()).isEqualTo("tok");
        verify(users).save(argThat(u -> !u.getPasswordHash().equals("secret123")));
    }

    @Test
    void loginRejectsBadPassword() {
        User u = new User();
        u.setId(1L); u.setUsername("bob");
        u.setPasswordHash(encoder.encode("secret123"));
        when(users.findByUsername("bob")).thenReturn(Optional.of(u));
        assertThatThrownBy(() -> service.login("bob", "wrong"))
                .isInstanceOf(ApiException.class);
    }
}
```

- [ ] **Step 3: 运行确认失败**

Run: `cd backend && ./gradlew test --tests AuthServiceTest`
Expected: FAIL（AuthService 不存在）

- [ ] **Step 4: 实现 AuthService**

`backend/src/main/java/com/voicenotes/service/AuthService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.User;
import com.voicenotes.dto.AuthDtos.AuthResponse;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.UserRepository;
import com.voicenotes.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse register(String username, String password) {
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);
        return new AuthResponse(jwt.generateToken(u.getUsername(), u.getId()), u.getUsername());
    }

    public AuthResponse login(String username, String password) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return new AuthResponse(jwt.generateToken(u.getUsername(), u.getId()), u.getUsername());
    }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `cd backend && ./gradlew test --tests AuthServiceTest`
Expected: PASS

- [ ] **Step 6: 写 AuthController**

`backend/src/main/java/com/voicenotes/controller/AuthController.java`:

```java
package com.voicenotes.controller;

import com.voicenotes.dto.AuthDtos.*;
import com.voicenotes.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req.username(), req.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.username(), req.password());
    }
}
```

- [ ] **Step 7: 写集成测试（MockMvc + H2）**

`backend/src/test/java/com/voicenotes/controller/AuthControllerIT.java`:

```java
package com.voicenotes.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {
    @Autowired MockMvc mvc;

    @Test
    void registerThenLogin() throws Exception {
        String body = "{\"username\":\"alice\",\"password\":\"secret123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("alice"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerValidationFails() throws Exception {
        String body = "{\"username\":\"ab\",\"password\":\"123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mvc.perform(post("/api/transcriptions"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 8: 运行确认通过**

Run: `cd backend && ./gradlew test --tests AuthControllerIT`
Expected: PASS（注：`protectedEndpointRequiresAuth` 验证未带 token 返回 401；该路径 controller 尚未建但 Security 会先拦截，返回 401 而非 404）

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/voicenotes/dto backend/src/main/java/com/voicenotes/exception backend/src/main/java/com/voicenotes/service/AuthService.java backend/src/main/java/com/voicenotes/controller/AuthController.java backend/src/test/java/com/voicenotes
git commit -m "feat: add auth service, controller, and global exception handling"
```

---

### Task 7: 存储服务与当前用户解析工具

**Files:**
- Create: `backend/src/main/java/com/voicenotes/service/StorageService.java`
- Create: `backend/src/main/java/com/voicenotes/security/CurrentUser.java`
- Test: `backend/src/test/java/com/voicenotes/service/StorageServiceTest.java`

- [ ] **Step 1: 写 StorageService 测试（先红）**

`backend/src/test/java/com/voicenotes/service/StorageServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class StorageServiceTest {
    StorageService storage;
    @TempDir Path tmp;

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        props.getStorage().setAudioDir(tmp.resolve("audio").toString());
        props.getStorage().setTtsDir(tmp.resolve("tts").toString());
        storage = new StorageService(props);
    }

    @Test
    void savesAudioAndReturnsPath() throws Exception {
        var file = new MockMultipartFile("file", "meeting.mp3", "audio/mpeg", "hello".getBytes());
        String path = storage.saveAudio(file);
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(path).endsWith(".mp3");
    }

    @Test
    void rejectsUnsupportedExtension() {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "x".getBytes());
        assertThatThrownBy(() -> storage.saveAudio(file)).isInstanceOf(Exception.class);
    }

    @Test
    void deleteRemovesFile() throws Exception {
        var file = new MockMultipartFile("file", "a.wav", "audio/wav", "x".getBytes());
        String path = storage.saveAudio(file);
        storage.delete(path);
        assertThat(Files.exists(Path.of(path))).isFalse();
    }

    @Test
    void savesTtsBytes() throws Exception {
        String path = storage.saveTts(1L, new byte[]{1, 2, 3});
        assertThat(Files.exists(Path.of(path))).isTrue();
        assertThat(path).endsWith(".mp3");
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd backend && ./gradlew test --tests StorageServiceTest`
Expected: FAIL

- [ ] **Step 3: 实现 StorageService**

`backend/src/main/java/com/voicenotes/service/StorageService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {
    private static final Set<String> ALLOWED = Set.of("mp3", "wav", "m4a");
    private final Path audioDir;
    private final Path ttsDir;

    public StorageService(AppProperties props) {
        this.audioDir = Path.of(props.getStorage().getAudioDir());
        this.ttsDir = Path.of(props.getStorage().getTtsDir());
    }

    public String saveAudio(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无法识别文件类型");
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "仅支持 mp3/wav/m4a");
        }
        try {
            Files.createDirectories(audioDir);
            Path target = audioDir.resolve(UUID.randomUUID() + "." + ext);
            file.transferTo(target.toAbsolutePath());
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存音频失败: " + e.getMessage());
        }
    }

    public String saveTts(Long transcriptionId, byte[] audioBytes) {
        try {
            Files.createDirectories(ttsDir);
            Path target = ttsDir.resolve("tts-" + transcriptionId + "-" + UUID.randomUUID() + ".mp3");
            Files.write(target, audioBytes);
            return target.toString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "保存朗读音频失败: " + e.getMessage());
        }
    }

    public void delete(String path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (IOException ignored) {
        }
    }
}
```

- [ ] **Step 4: 实现 CurrentUser 工具（从 SecurityContext 取 userId）**

`backend/src/main/java/com/voicenotes/security/CurrentUser.java`:

```java
package com.voicenotes.security;

import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }

    public Long requireUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails ud)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未认证");
        }
        return users.findByUsername(ud.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户不存在"))
                .getId();
    }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `cd backend && ./gradlew test --tests StorageServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/StorageService.java backend/src/main/java/com/voicenotes/security/CurrentUser.java backend/src/test/java/com/voicenotes/service/StorageServiceTest.java
git commit -m "feat: add storage service and current-user resolver"
```

### Task 8: 转写 DTO 与上传接口（TranscriptionController 上传部分）

**Files:**
- Create: `backend/src/main/java/com/voicenotes/dto/TranscriptionDtos.java`
- Create: `backend/src/main/java/com/voicenotes/service/TranscriptionService.java`（仅上传相关，后续 Task 扩展）
- Create: `backend/src/main/java/com/voicenotes/controller/TranscriptionController.java`（仅上传 + 查询）
- Test: `backend/src/test/java/com/voicenotes/controller/TranscriptionUploadIT.java`

- [ ] **Step 1: 写 DTO**

`backend/src/main/java/com/voicenotes/dto/TranscriptionDtos.java`:

```java
package com.voicenotes.dto;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;

import java.time.Instant;

public class TranscriptionDtos {
    public record DetailResponse(
            Long id,
            String originalFilename,
            TranscriptionStatus status,
            String transcriptText,
            String summaryMarkdown,
            boolean hasTtsAudio,
            SummaryTemplate template,
            String errorMessage,
            Instant createdAt) {
        public static DetailResponse from(Transcription t) {
            return new DetailResponse(
                    t.getId(), t.getOriginalFilename(), t.getStatus(),
                    t.getTranscriptText(), t.getSummaryMarkdown(),
                    t.getTtsAudioPath() != null,
                    t.getTemplate(), t.getErrorMessage(), t.getCreatedAt());
        }
    }

    public record ListItem(
            Long id, String originalFilename, TranscriptionStatus status,
            SummaryTemplate template, Instant createdAt) {
        public static ListItem from(Transcription t) {
            return new ListItem(t.getId(), t.getOriginalFilename(), t.getStatus(),
                    t.getTemplate(), t.getCreatedAt());
        }
    }

    public record UploadResponse(Long id, TranscriptionStatus status) {}
}
```

- [ ] **Step 2: 写 TranscriptionService（上传 + 查询 + 列表，越权校验）**

`backend/src/main/java/com/voicenotes/service/TranscriptionService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TranscriptionService {
    private final TranscriptionRepository repo;
    private final StorageService storage;

    public TranscriptionService(TranscriptionRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public Transcription upload(Long userId, MultipartFile file, SummaryTemplate template) {
        String path = storage.saveAudio(file);
        Transcription t = new Transcription();
        t.setUserId(userId);
        t.setOriginalFilename(file.getOriginalFilename());
        t.setAudioPath(path);
        t.setTemplate(template == null ? SummaryTemplate.GENERAL : template);
        t.setStatus(TranscriptionStatus.UPLOADED);
        return repo.save(t);
    }

    public Transcription getOwned(Long userId, Long id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权访问或记录不存在"));
    }

    public List<Transcription> list(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(Long userId, Long id) {
        Transcription t = getOwned(userId, id);
        storage.delete(t.getAudioPath());
        storage.delete(t.getTtsAudioPath());
        repo.delete(t);
    }

    public Transcription save(Transcription t) {
        return repo.save(t);
    }
}
```

- [ ] **Step 3: 写 TranscriptionController（上传 / 详情 / 列表 / 删除）**

`backend/src/main/java/com/voicenotes/controller/TranscriptionController.java`:

```java
package com.voicenotes.controller;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.dto.TranscriptionDtos.*;
import com.voicenotes.security.CurrentUser;
import com.voicenotes.service.TranscriptionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {
    private final TranscriptionService service;
    private final CurrentUser currentUser;

    public TranscriptionController(TranscriptionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "template", required = false) SummaryTemplate template) {
        Long uid = currentUser.requireUserId();
        Transcription t = service.upload(uid, file, template);
        return new UploadResponse(t.getId(), t.getStatus());
    }

    @GetMapping("/{id}")
    public DetailResponse detail(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        return DetailResponse.from(service.getOwned(uid, id));
    }

    @GetMapping
    public List<ListItem> list() {
        Long uid = currentUser.requireUserId();
        return service.list(uid).stream().map(ListItem::from).toList();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        service.delete(uid, id);
    }
}
```

- [ ] **Step 4: 写上传集成测试（先注册拿 token，再带 token 上传）**

`backend/src/test/java/com/voicenotes/controller/TranscriptionUploadIT.java`:

```java
package com.voicenotes.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TranscriptionUploadIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String registerAndToken(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode node = om.readTree(json);
        return node.get("token").asText();
    }

    @Test
    void uploadStoresRecord() throws Exception {
        String token = registerAndToken("upUser");
        var file = new MockMultipartFile("file", "meeting.mp3", "audio/mpeg", "fakeaudio".getBytes());

        String resp = mvc.perform(multipart("/api/transcriptions").file(file)
                        .param("template", "MEETING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andReturn().getResponse().getContentAsString();

        long id = om.readTree(resp).get("id").asLong();
        mvc.perform(get("/api/transcriptions/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("meeting.mp3"))
                .andExpect(jsonPath("$.template").value("MEETING"));
    }

    @Test
    void rejectsBadExtension() throws Exception {
        String token = registerAndToken("badExtUser");
        var file = new MockMultipartFile("file", "x.txt", "text/plain", "x".getBytes());
        mvc.perform(multipart("/api/transcriptions").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotAccessOthersRecord() throws Exception {
        String tokenA = registerAndToken("ownerUser");
        var file = new MockMultipartFile("file", "a.mp3", "audio/mpeg", "x".getBytes());
        String resp = mvc.perform(multipart("/api/transcriptions").file(file)
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(resp).get("id").asLong();

        String tokenB = registerAndToken("intruderUser");
        mvc.perform(get("/api/transcriptions/" + id).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `cd backend && ./gradlew test --tests TranscriptionUploadIT`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/voicenotes/dto/TranscriptionDtos.java backend/src/main/java/com/voicenotes/service/TranscriptionService.java backend/src/main/java/com/voicenotes/controller/TranscriptionController.java backend/src/test/java/com/voicenotes/controller/TranscriptionUploadIT.java
git commit -m "feat: add audio upload, detail, list, delete endpoints"
```

---

### Task 9: DashScope 转写服务（实时识别喂本地文件）

> **集成约束**：DashScope 批量识别需公网 URL，本地文件够不到。这里用实时识别 SDK（`Recognition`）把本地音频流式喂入，后端阻塞收集所有分段结果拼成全文。封装成同步方法 `transcribe(path)`，对调用方表现为「给路径、返回全文」。

**Files:**
- Create: `backend/src/main/java/com/voicenotes/service/DashScopeService.java`（先只做 ASR 部分）
- Test: `backend/src/test/java/com/voicenotes/service/DashScopeServiceTest.java`（用可重写的「发送方法」做单元测试）

- [ ] **Step 1: 写 DashScopeService（ASR 部分），把「真正调用 SDK」隔离成 protected 方法便于测试**

`backend/src/main/java/com/voicenotes/service/DashScopeService.java`:

```java
package com.voicenotes.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DashScopeService {
    private final AppProperties props;

    public DashScopeService(AppProperties props) {
        this.props = props;
    }

    /** 把本地音频文件转写成全文。失败抛 ApiException。 */
    public String transcribe(String audioPath) {
        requireKey();
        File f = new File(audioPath);
        if (!f.exists()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "音频文件不存在: " + audioPath);
        }
        try {
            return doRecognize(audioPath);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "转写失败: " + e.getMessage());
        }
    }

    /** 隔离真实 SDK 调用，测试时可覆盖。 */
    protected String doRecognize(String audioPath) throws Exception {
        Recognition recognizer = new Recognition();
        RecognitionParam param = RecognitionParam.builder()
                .model(props.getDashscope().getAsrModel())
                .format(detectFormat(audioPath))
                .sampleRate(16000)
                .apiKey(props.getDashscope().getApiKey())
                .build();
        String result = recognizer.call(param, new File(audioPath));
        if (result == null || result.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "转写返回空结果");
        }
        return result;
    }

    protected String detectFormat(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".wav")) return "wav";
        if (lower.endsWith(".m4a")) return "m4a";
        return "mp3";
    }

    protected void requireKey() {
        String key = props.getDashscope().getApiKey();
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "未配置 DASHSCOPE_API_KEY");
        }
    }
}
```

> **注意**：`Recognition.call(param, File)` 的确切签名取决于 dashscope-sdk-java 版本。实现时若该重载不存在，改用 SDK 文档中「文件转写」的对应 API（思路一致：把本地文件喂入、收集结果）。`doRecognize` 被隔离正是为了让这处适配不影响其余代码与测试。

- [ ] **Step 2: 写单元测试（覆盖 doRecognize，避免真调云端）**

`backend/src/test/java/com/voicenotes/service/DashScopeServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class DashScopeServiceTest {
    AppProperties props;
    @TempDir Path tmp;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getDashscope().setApiKey("test-key");
    }

    @Test
    void transcribeReturnsStubbedText() throws Exception {
        Path audio = tmp.resolve("a.mp3");
        Files.writeString(audio, "x");
        DashScopeService svc = new DashScopeService(props) {
            @Override protected String doRecognize(String audioPath) { return "你好 世界"; }
        };
        assertThat(svc.transcribe(audio.toString())).isEqualTo("你好 世界");
    }

    @Test
    void missingKeyThrows() {
        props.getDashscope().setApiKey("");
        DashScopeService svc = new DashScopeService(props);
        assertThatThrownBy(() -> svc.transcribe("whatever.mp3"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("DASHSCOPE_API_KEY");
    }

    @Test
    void missingFileThrows() {
        DashScopeService svc = new DashScopeService(props);
        assertThatThrownBy(() -> svc.transcribe(tmp.resolve("nope.mp3").toString()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void detectFormat() {
        DashScopeService svc = new DashScopeService(props);
        assertThat(svc.detectFormat("/x/a.WAV")).isEqualTo("wav");
        assertThat(svc.detectFormat("/x/a.m4a")).isEqualTo("m4a");
        assertThat(svc.detectFormat("/x/a.mp3")).isEqualTo("mp3");
    }
}
```

- [ ] **Step 3: 运行确认通过**

Run: `cd backend && ./gradlew test --tests DashScopeServiceTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/DashScopeService.java backend/src/test/java/com/voicenotes/service/DashScopeServiceTest.java
git commit -m "feat: add DashScope ASR service (local file recognition)"
```

---

### Task 10: 转写编排与触发接口（状态流转 + 异步执行）

**Files:**
- Modify: `backend/src/main/java/com/voicenotes/service/TranscriptionService.java`（加 `runTranscription`）
- Modify: `backend/src/main/java/com/voicenotes/controller/TranscriptionController.java`（加 `POST /{id}/transcribe`）
- Modify: `backend/src/main/java/com/voicenotes/VoiceNotesApplication.java`（加 `@EnableAsync`）
- Test: `backend/src/test/java/com/voicenotes/service/TranscriptionFlowTest.java`

- [ ] **Step 1: 给启动类加 @EnableAsync**

修改 `VoiceNotesApplication.java`，在类上增加注解：

```java
package com.voicenotes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class VoiceNotesApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoiceNotesApplication.class, args);
    }
}
```

- [ ] **Step 2: 给 TranscriptionService 注入 DashScopeService 并加转写编排方法**

修改 `TranscriptionService.java`：构造函数加入 `DashScopeService`，并新增方法。完整改动后的相关部分：

```java
// 字段与构造函数改为：
private final TranscriptionRepository repo;
private final StorageService storage;
private final DashScopeService dashScope;

public TranscriptionService(TranscriptionRepository repo, StorageService storage, DashScopeService dashScope) {
    this.repo = repo;
    this.storage = storage;
    this.dashScope = dashScope;
}

// 新增：触发转写（同步执行核心逻辑，便于测试）
public void runTranscription(Long userId, Long id) {
    Transcription t = getOwned(userId, id);
    if (t.getStatus() == TranscriptionStatus.TRANSCRIBING) {
        throw new ApiException(HttpStatus.CONFLICT, "正在转写中");
    }
    t.setStatus(TranscriptionStatus.TRANSCRIBING);
    t.setErrorMessage(null);
    repo.save(t);
    try {
        String text = dashScope.transcribe(t.getAudioPath());
        t.setTranscriptText(text);
        t.setStatus(TranscriptionStatus.TRANSCRIBED);
        repo.save(t);
    } catch (Exception e) {
        t.setStatus(TranscriptionStatus.FAILED);
        t.setErrorMessage(e.getMessage());
        repo.save(t);
    }
}
```

需要在文件顶部确保已 import `com.voicenotes.domain.TranscriptionStatus`、`com.voicenotes.exception.ApiException`、`org.springframework.http.HttpStatus`（part-d Task 8 已有部分，按缺补齐）。

- [ ] **Step 3: 给 Controller 加触发接口**

修改 `TranscriptionController.java`，新增方法：

```java
@PostMapping("/{id}/transcribe")
public DetailResponse transcribe(@PathVariable Long id) {
    Long uid = currentUser.requireUserId();
    service.runTranscription(uid, id);
    return DetailResponse.from(service.getOwned(uid, id));
}
```

> **说明**：本计划为简化与可测试性，转写在请求线程内同步执行（短录音演示足够）。若要异步，把 `runTranscription` 拆成 `@Async` 方法立即返回、前端轮询 `GET /{id}` 看状态；`@EnableAsync` 已就位。

- [ ] **Step 4: 写流程测试（mock DashScopeService）**

`backend/src/test/java/com/voicenotes/service/TranscriptionFlowTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.repository.TranscriptionRepository;
import org.junit.jupiter.api.*;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TranscriptionFlowTest {
    TranscriptionRepository repo;
    StorageService storage;
    DashScopeService dash;
    TranscriptionService service;

    @BeforeEach
    void setup() {
        repo = mock(TranscriptionRepository.class);
        storage = mock(StorageService.class);
        dash = mock(DashScopeService.class);
        service = new TranscriptionService(repo, storage, dash);
        when(repo.save(any(Transcription.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Transcription owned() {
        Transcription t = new Transcription();
        t.setId(5L); t.setUserId(1L); t.setAudioPath("/x/a.mp3");
        t.setStatus(TranscriptionStatus.UPLOADED);
        when(repo.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void successSetsTranscribed() {
        Transcription t = owned();
        when(dash.transcribe("/x/a.mp3")).thenReturn("全文内容");
        service.runTranscription(1L, 5L);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.TRANSCRIBED);
        assertThat(t.getTranscriptText()).isEqualTo("全文内容");
    }

    @Test
    void failureSetsFailedWithMessage() {
        Transcription t = owned();
        when(dash.transcribe("/x/a.mp3")).thenThrow(new RuntimeException("boom"));
        service.runTranscription(1L, 5L);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
        assertThat(t.getErrorMessage()).contains("boom");
    }
}
```

- [ ] **Step 5: 运行确认通过**

Run: `cd backend && ./gradlew test --tests TranscriptionFlowTest`
Expected: PASS

- [ ] **Step 6: 跑全量测试回归**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/voicenotes backend/src/test/java/com/voicenotes/service/TranscriptionFlowTest.java
git commit -m "feat: add transcription orchestration with status flow"
```

### Task 11: 概括 Prompt 模板服务

**Files:**
- Create: `backend/src/main/java/com/voicenotes/service/PromptTemplateService.java`
- Test: `backend/src/test/java/com/voicenotes/service/PromptTemplateServiceTest.java`

- [ ] **Step 1: 写测试（先红）**

`backend/src/test/java/com/voicenotes/service/PromptTemplateServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {
    PromptTemplateService svc = new PromptTemplateService();

    @Test
    void meetingPromptMentionsActionItems() {
        String p = svc.buildPrompt(SummaryTemplate.MEETING, "今天讨论了排期");
        assertThat(p).contains("今天讨论了排期");
        assertThat(p).contains("待办");
        assertThat(p).contains("Markdown");
    }

    @Test
    void lecturePromptMentionsKeyPoints() {
        String p = svc.buildPrompt(SummaryTemplate.LECTURE, "讲了傅里叶变换");
        assertThat(p).contains("讲了傅里叶变换");
        assertThat(p).contains("知识点");
    }

    @Test
    void generalPromptFallback() {
        String p = svc.buildPrompt(SummaryTemplate.GENERAL, "随便聊聊");
        assertThat(p).contains("随便聊聊");
        assertThat(p).contains("概括");
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd backend && ./gradlew test --tests PromptTemplateServiceTest`
Expected: FAIL

- [ ] **Step 3: 实现 PromptTemplateService**

`backend/src/main/java/com/voicenotes/service/PromptTemplateService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    public String buildPrompt(SummaryTemplate template, String transcript) {
        String instruction = switch (template) {
            case MEETING -> """
                你是会议纪要助手。请根据下面的会议转写文字，输出一份结构化的 Markdown 会议纪要，包含：
                # 会议纪要
                ## 核心结论
                ## 讨论要点
                ## 待办事项（action items，标注负责人，如转写未提及则写"未指定"）
                ## 关键词
                只输出 Markdown，不要额外说明。""";
            case LECTURE -> """
                你是课堂笔记助手。请根据下面的课程转写文字，输出一份结构化的 Markdown 课堂笔记，包含：
                # 课堂笔记
                ## 主题概述
                ## 知识点（分点列出，重要概念加粗）
                ## 重点与难点
                ## 关键词
                只输出 Markdown，不要额外说明。""";
            case GENERAL -> """
                你是内容概括助手。请根据下面的转写文字，输出一份结构化的 Markdown 概括，包含：
                # 内容概括
                ## 摘要
                ## 要点（分点列出）
                ## 关键词
                只输出 Markdown，不要额外说明。""";
        };
        return instruction + "\n\n---\n转写文字：\n" + transcript;
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `cd backend && ./gradlew test --tests PromptTemplateServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/PromptTemplateService.java backend/src/test/java/com/voicenotes/service/PromptTemplateServiceTest.java
git commit -m "feat: add prompt template service for summary templates"
```

---

### Task 12: Qwen 流式概括（DashScopeService 扩展）

> Qwen 通过 dashscope-sdk-java 的 `Generation` 流式接口返回增量内容。把「真正调用 SDK」隔离成 protected 方法，用回调 `Consumer<String>` 把每个增量片段交给上层（SSE 推送 + 累积入库）。

**Files:**
- Modify: `backend/src/main/java/com/voicenotes/service/DashScopeService.java`（加 `streamSummary`）
- Test: `backend/src/test/java/com/voicenotes/service/DashScopeSummaryTest.java`

- [ ] **Step 1: 给 DashScopeService 加流式概括方法**

在 `DashScopeService.java` 中新增（保留已有 ASR 代码）：

```java
// 顶部新增 import
import java.util.function.Consumer;

/**
 * 流式概括。每个增量片段通过 onChunk 回调交出；返回拼接后的完整文本。
 * 失败抛 ApiException。
 */
public String streamSummary(String prompt, Consumer<String> onChunk) {
    requireKey();
    StringBuilder full = new StringBuilder();
    try {
        doStream(prompt, chunk -> {
            full.append(chunk);
            onChunk.accept(chunk);
        });
    } catch (ApiException e) {
        throw e;
    } catch (Exception e) {
        throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "概括失败: " + e.getMessage());
    }
    if (full.length() == 0) {
        throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "概括返回空结果");
    }
    return full.toString();
}

/** 隔离真实 Qwen 流式 SDK 调用，测试时可覆盖。 */
protected void doStream(String prompt, Consumer<String> onChunk) throws Exception {
    com.alibaba.dashscope.aigc.generation.Generation gen =
            new com.alibaba.dashscope.aigc.generation.Generation();
    com.alibaba.dashscope.aigc.generation.GenerationParam param =
            com.alibaba.dashscope.aigc.generation.GenerationParam.builder()
                    .apiKey(props.getDashscope().getApiKey())
                    .model(props.getDashscope().getLlmModel())
                    .messages(java.util.List.of(
                            com.alibaba.dashscope.common.Message.builder()
                                    .role(com.alibaba.dashscope.common.Role.USER.getValue())
                                    .content(prompt).build()))
                    .resultFormat(com.alibaba.dashscope.aigc.generation.GenerationParam.ResultFormat.MESSAGE)
                    .incrementalOutput(true)
                    .build();

    io.reactivex.Flowable<com.alibaba.dashscope.aigc.generation.GenerationResult> flow = gen.streamCall(param);
    flow.blockingForEach(result -> {
        String piece = result.getOutput().getChoices().get(0).getMessage().getContent();
        if (piece != null && !piece.isEmpty()) {
            onChunk.accept(piece);
        }
    });
}
```

> **注意**：流式 API 的确切类型/方法（`streamCall`、`incrementalOutput`、`GenerationResult` 取值路径）随 SDK 版本略有差异。`doStream` 被隔离即为适配这处而不影响上层逻辑与测试。

- [ ] **Step 2: 写单元测试（覆盖 doStream，模拟分片）**

`backend/src/test/java/com/voicenotes/service/DashScopeSummaryTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;

class DashScopeSummaryTest {
    AppProperties props;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getDashscope().setApiKey("test-key");
    }

    @Test
    void streamsChunksAndReturnsFullText() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) {
                onChunk.accept("# 概");
                onChunk.accept("括\n");
                onChunk.accept("- 要点");
            }
        };
        List<String> received = new ArrayList<>();
        String full = svc.streamSummary("prompt", received::add);
        assertThat(received).containsExactly("# 概", "括\n", "- 要点");
        assertThat(full).isEqualTo("# 概括\n- 要点");
    }

    @Test
    void emptyResultThrows() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) { /* 不发 */ }
        };
        assertThatThrownBy(() -> svc.streamSummary("p", c -> {}))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void underlyingErrorWrapped() {
        DashScopeService svc = new DashScopeService(props) {
            @Override protected void doStream(String prompt, Consumer<String> onChunk) {
                throw new RuntimeException("network");
            }
        };
        assertThatThrownBy(() -> svc.streamSummary("p", c -> {}))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("概括失败");
    }
}
```

- [ ] **Step 3: 运行确认通过**

Run: `cd backend && ./gradlew test --tests DashScopeSummaryTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/DashScopeService.java backend/src/test/java/com/voicenotes/service/DashScopeSummaryTest.java
git commit -m "feat: add Qwen streaming summary to DashScope service"
```

---

### Task 13: 概括编排（SummaryService）

> 编排：取转写文本 → 拼 prompt → 调流式概括 → 每片回调推给上层 → 完成后入库、状态 DONE。SSE 推送细节在 Controller 层（Task 14）。这里返回一个「带回调的执行方法」。

**Files:**
- Create: `backend/src/main/java/com/voicenotes/service/SummaryService.java`
- Test: `backend/src/test/java/com/voicenotes/service/SummaryServiceTest.java`

- [ ] **Step 1: 写测试（先红）**

`backend/src/test/java/com/voicenotes/service/SummaryServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SummaryServiceTest {
    TranscriptionRepository repo;
    DashScopeService dash;
    PromptTemplateService prompts;
    SummaryService service;

    @BeforeEach
    void setup() {
        repo = mock(TranscriptionRepository.class);
        dash = mock(DashScopeService.class);
        prompts = new PromptTemplateService();
        service = new SummaryService(repo, dash, prompts);
        when(repo.save(any(Transcription.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Transcription transcribed() {
        Transcription t = new Transcription();
        t.setId(7L); t.setUserId(1L);
        t.setStatus(TranscriptionStatus.TRANSCRIBED);
        t.setTranscriptText("会议内容");
        t.setTemplate(SummaryTemplate.MEETING);
        when(repo.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void streamsAndPersistsSummary() {
        Transcription t = transcribed();
        when(dash.streamSummary(anyString(), any())).thenAnswer(inv -> {
            Consumer<String> cb = inv.getArgument(1);
            cb.accept("# 会议纪要\n");
            cb.accept("## 核心结论");
            return "# 会议纪要\n## 核心结论";
        });

        List<String> chunks = new ArrayList<>();
        service.streamSummary(1L, 7L, chunks::add);

        assertThat(chunks).containsExactly("# 会议纪要\n", "## 核心结论");
        assertThat(t.getSummaryMarkdown()).isEqualTo("# 会议纪要\n## 核心结论");
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.DONE);
    }

    @Test
    void rejectsWhenNotTranscribed() {
        Transcription t = new Transcription();
        t.setId(8L); t.setUserId(1L);
        t.setStatus(TranscriptionStatus.UPLOADED);
        when(repo.findByIdAndUserId(8L, 1L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.streamSummary(1L, 8L, c -> {}))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void failureMarksFailed() {
        Transcription t = transcribed();
        when(dash.streamSummary(anyString(), any())).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> service.streamSummary(1L, 7L, c -> {}))
                .isInstanceOf(RuntimeException.class);
        assertThat(t.getStatus()).isEqualTo(TranscriptionStatus.FAILED);
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd backend && ./gradlew test --tests SummaryServiceTest`
Expected: FAIL

- [ ] **Step 3: 实现 SummaryService**

`backend/src/main/java/com/voicenotes/service/SummaryService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.exception.ApiException;
import com.voicenotes.repository.TranscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class SummaryService {
    private final TranscriptionRepository repo;
    private final DashScopeService dashScope;
    private final PromptTemplateService prompts;

    public SummaryService(TranscriptionRepository repo, DashScopeService dashScope, PromptTemplateService prompts) {
        this.repo = repo;
        this.dashScope = dashScope;
        this.prompts = prompts;
    }

    /** 流式概括：每片通过 onChunk 交出，完成后入库并置 DONE。失败置 FAILED 并抛出。 */
    public void streamSummary(Long userId, Long id, Consumer<String> onChunk) {
        Transcription t = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权访问或记录不存在"));
        if (t.getTranscriptText() == null || t.getTranscriptText().isBlank()) {
            throw new ApiException(HttpStatus.CONFLICT, "尚未完成转写，无法概括");
        }
        t.setStatus(TranscriptionStatus.SUMMARIZING);
        t.setErrorMessage(null);
        repo.save(t);
        try {
            String prompt = prompts.buildPrompt(t.getTemplate(), t.getTranscriptText());
            String full = dashScope.streamSummary(prompt, onChunk);
            t.setSummaryMarkdown(full);
            t.setStatus(TranscriptionStatus.DONE);
            repo.save(t);
        } catch (RuntimeException e) {
            t.setStatus(TranscriptionStatus.FAILED);
            t.setErrorMessage(e.getMessage());
            repo.save(t);
            throw e;
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `cd backend && ./gradlew test --tests SummaryServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/SummaryService.java backend/src/test/java/com/voicenotes/service/SummaryServiceTest.java
git commit -m "feat: add summary orchestration service"
```

---

### Task 14: SSE 概括流接口（SummaryController）

**Files:**
- Create: `backend/src/main/java/com/voicenotes/controller/SummaryController.java`（先只做 SSE 概括，TTS 在 Task 16 扩展）
- Test: `backend/src/test/java/com/voicenotes/controller/SummaryStreamIT.java`

- [ ] **Step 1: 写 SummaryController（SSE）**

`backend/src/main/java/com/voicenotes/controller/SummaryController.java`:

```java
package com.voicenotes.controller;

import com.voicenotes.security.CurrentUser;
import com.voicenotes.service.SummaryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {
    private final SummaryService summaryService;
    private final CurrentUser currentUser;

    public SummaryController(SummaryService summaryService, CurrentUser currentUser) {
        this.summaryService = summaryService;
        this.currentUser = currentUser;
    }

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        Long uid = currentUser.requireUserId();
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                summaryService.streamSummary(uid, id, chunk -> {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(chunk));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.complete();
            }
        });
        return emitter;
    }
}
```

> SSE 鉴权：`EventSource` 不能设 Authorization header，前端通过 `?token=` 传 JWT，`JwtAuthFilter`（Task 5）已支持从 query 读 token。

- [ ] **Step 2: 写集成测试（mock SummaryService，验证 SSE 输出片段与完成事件）**

`backend/src/test/java/com/voicenotes/controller/SummaryStreamIT.java`:

```java
package com.voicenotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicenotes.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SummaryStreamIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean SummaryService summaryService;

    private String token(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    @Test
    void streamsChunksAndDone() throws Exception {
        doAnswer(inv -> {
            Consumer<String> cb = inv.getArgument(2);
            cb.accept("# 概括\n");
            cb.accept("- 要点");
            return null;
        }).when(summaryService).streamSummary(anyLong(), eq(7L), any());

        String tk = token("sseUser");
        MvcResult res = mvc.perform(get("/api/summaries/7/stream").param("token", tk)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(res))
                .andExpect(status().isOk());

        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("# 概括").contains("- 要点").contains("[DONE]");
    }
}
```

> 若运行环境的 Spring Boot 版本不支持 `@MockitoBean`（3.4+），改用 `@MockBean`（已弃用但可用）。二者语义一致。

- [ ] **Step 3: 运行确认通过**

Run: `cd backend && ./gradlew test --tests SummaryStreamIT`
Expected: PASS

- [ ] **Step 4: 跑全量回归**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voicenotes/controller/SummaryController.java backend/src/test/java/com/voicenotes/controller/SummaryStreamIT.java
git commit -m "feat: add SSE streaming summary endpoint"
```

### Task 15: TTS 朗读服务（TtsService）

> speech-2.8-hd 走 OpenAI 兼容 `/v1/audio/speech`，经中转站 base URL。把概括 Markdown 去标记成纯文本，按 `maxChars` 切分，逐段合成 mp3 字节，拼接成整段音频。把「真正 HTTP 调用」隔离成 protected 方法便于测试。

**Files:**
- Create: `backend/src/main/java/com/voicenotes/service/TtsService.java`
- Test: `backend/src/test/java/com/voicenotes/service/TtsServiceTest.java`

- [ ] **Step 1: 写测试（先红）**

`backend/src/test/java/com/voicenotes/service/TtsServiceTest.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class TtsServiceTest {
    AppProperties props;

    @BeforeEach
    void setup() {
        props = new AppProperties();
        props.getTts().setBaseUrl("http://relay.example/v1");
        props.getTts().setApiKey("test-key");
        props.getTts().setMaxChars(10);
    }

    @Test
    void stripsMarkdownToPlainText() {
        TtsService svc = new TtsService(props);
        String plain = svc.toPlainText("# 标题\n- **要点**一\n- 要点二\n`code`");
        assertThat(plain).doesNotContain("#").doesNotContain("*").doesNotContain("`");
        assertThat(plain).contains("标题").contains("要点一").contains("要点二");
    }

    @Test
    void splitsByMaxChars() {
        TtsService svc = new TtsService(props);
        List<String> parts = svc.splitText("一二三四五六七八九十一二三四五"); // 15 chars, max 10
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).length()).isLessThanOrEqualTo(10);
    }

    @Test
    void synthesizeConcatenatesSegmentBytes() {
        List<String> calls = new ArrayList<>();
        TtsService svc = new TtsService(props) {
            @Override protected byte[] requestSpeech(String text) {
                calls.add(text);
                return text.getBytes();
            }
        };
        props.getTts().setMaxChars(5);
        byte[] out = svc.synthesize("一二三四五六七"); // 7 chars -> 2 段
        assertThat(calls).hasSize(2);
        assertThat(new String(out)).isEqualTo("一二三四五六七");
    }

    @Test
    void missingConfigThrows() {
        props.getTts().setApiKey("");
        TtsService svc = new TtsService(props);
        assertThatThrownBy(() -> svc.synthesize("文本"))
                .isInstanceOf(ApiException.class);
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `cd backend && ./gradlew test --tests TtsServiceTest`
Expected: FAIL

- [ ] **Step 3: 实现 TtsService**

`backend/src/main/java/com/voicenotes/service/TtsService.java`:

```java
package com.voicenotes.service;

import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import okhttp3.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TtsService {
    private final AppProperties props;
    private final OkHttpClient http;

    public TtsService(AppProperties props) {
        this.props = props;
        this.http = new OkHttpClient.Builder()
                .callTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /** Markdown -> 纯文本（去掉标记符号，便于朗读）。 */
    public String toPlainText(String markdown) {
        if (markdown == null) return "";
        return markdown
                .replaceAll("(?m)^#{1,6}\\s*", "")   // 标题
                .replaceAll("\\*\\*([^*]+)\\*\\*", "$1") // 粗体
                .replaceAll("\\*([^*]+)\\*", "$1")       // 斜体
                .replaceAll("`([^`]+)`", "$1")           // 行内代码
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")    // 列表符号
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "")  // 有序列表
                .replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1") // 链接
                .replaceAll("\\n{2,}", "\n")
                .trim();
    }

    /** 按 maxChars 切分（尽量在换行/句号处断，简单实现按长度切）。 */
    public List<String> splitText(String text) {
        int max = props.getTts().getMaxChars();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < text.length(); i += max) {
            parts.add(text.substring(i, Math.min(text.length(), i + max)));
        }
        if (parts.isEmpty()) parts.add("");
        return parts;
    }

    /** 合成整段音频字节。 */
    public byte[] synthesize(String markdown) {
        requireConfig();
        String plain = toPlainText(markdown);
        if (plain.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无可朗读内容");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (String seg : splitText(plain)) {
                if (seg.isBlank()) continue;
                out.write(requestSpeech(seg));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "朗读生成失败: " + e.getMessage());
        }
        return out.toByteArray();
    }

    /** 隔离真实 HTTP 调用，测试时可覆盖。 */
    protected byte[] requestSpeech(String text) throws Exception {
        String url = props.getTts().getBaseUrl().replaceAll("/+$", "") + "/audio/speech";
        String json = "{"
                + "\"model\":\"" + props.getTts().getModel() + "\","
                + "\"input\":" + jsonString(text) + ","
                + "\"voice\":\"" + props.getTts().getVoice() + "\","
                + "\"response_format\":\"mp3\""
                + "}";
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + props.getTts().getApiKey())
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "TTS 服务返回 " + resp.code());
            }
            return resp.body().bytes();
        }
    }

    private void requireConfig() {
        var tts = props.getTts();
        if (tts.getBaseUrl() == null || tts.getBaseUrl().isBlank()
                || tts.getApiKey() == null || tts.getApiKey().isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "未配置 TTS_BASE_URL / TTS_API_KEY");
        }
    }

    private String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `cd backend && ./gradlew test --tests TtsServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/voicenotes/service/TtsService.java backend/src/test/java/com/voicenotes/service/TtsServiceTest.java
git commit -m "feat: add TTS service with markdown stripping and segment concatenation"
```

---

### Task 16: TTS 生成/播放接口 + Markdown 下载（Controller 扩展）

**Files:**
- Modify: `backend/src/main/java/com/voicenotes/service/TranscriptionService.java`（加 `generateSpeech`）
- Modify: `backend/src/main/java/com/voicenotes/controller/SummaryController.java`（加 TTS 生成/播放）
- Modify: `backend/src/main/java/com/voicenotes/controller/TranscriptionController.java`（加 `.md` 下载）
- Create: `backend/src/main/java/com/voicenotes/dto/SpeechDtos.java`
- Test: `backend/src/test/java/com/voicenotes/controller/SpeechAndDownloadIT.java`

- [ ] **Step 1: 写 SpeechDtos**

`backend/src/main/java/com/voicenotes/dto/SpeechDtos.java`:

```java
package com.voicenotes.dto;

public class SpeechDtos {
    public record SpeechResponse(Long id, String audioUrl) {}
}
```

- [ ] **Step 2: 给 TranscriptionService 加 generateSpeech（复用已有则不重复调用）**

在 `TranscriptionService.java` 加入字段 `TtsService tts` 与方法。构造函数追加参数：

```java
// 构造函数改为注入四个依赖：
private final TranscriptionRepository repo;
private final StorageService storage;
private final DashScopeService dashScope;
private final TtsService tts;

public TranscriptionService(TranscriptionRepository repo, StorageService storage,
                            DashScopeService dashScope, TtsService tts) {
    this.repo = repo;
    this.storage = storage;
    this.dashScope = dashScope;
    this.tts = tts;
}

/** 生成（或复用）朗读音频，返回音频文件路径。 */
public Transcription generateSpeech(Long userId, Long id) {
    Transcription t = getOwned(userId, id);
    if (t.getSummaryMarkdown() == null || t.getSummaryMarkdown().isBlank()) {
        throw new ApiException(HttpStatus.CONFLICT, "尚无概括内容，无法朗读");
    }
    if (t.getTtsAudioPath() != null && new java.io.File(t.getTtsAudioPath()).exists()) {
        return t; // 复用已生成
    }
    byte[] audio = tts.synthesize(t.getSummaryMarkdown());
    String path = storage.saveTts(t.getId(), audio);
    t.setTtsAudioPath(path);
    return repo.save(t);
}
```

> **注意**：`TranscriptionFlowTest`（Task 10）构造 `TranscriptionService` 时只传了 3 个参数，本任务改为 4 参后需更新该测试的构造调用，新增 `mock(TtsService.class)` 传入。请在本步同时修改 `TranscriptionFlowTest` 的 `setup()`：声明 `TtsService tts = mock(TtsService.class);` 并改为 `new TranscriptionService(repo, storage, dash, tts);`

- [ ] **Step 3: 给 SummaryController 加 TTS 生成/播放接口**

在 `SummaryController.java` 注入 `TranscriptionService` 并新增方法（构造函数追加参数）：

```java
// 顶部 import
import com.voicenotes.domain.Transcription;
import com.voicenotes.dto.SpeechDtos.SpeechResponse;
import com.voicenotes.service.TranscriptionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

// 字段与构造（在已有基础上追加 transcriptionService）
private final TranscriptionService transcriptionService;
// 构造函数参数追加 TranscriptionService transcriptionService 并赋值

@PostMapping("/{id}/speech")
public SpeechResponse generateSpeech(@PathVariable Long id) {
    Long uid = currentUser.requireUserId();
    transcriptionService.generateSpeech(uid, id);
    return new SpeechResponse(id, "/api/summaries/" + id + "/speech");
}

@GetMapping("/{id}/speech")
public ResponseEntity<FileSystemResource> playSpeech(@PathVariable Long id) {
    Long uid = currentUser.requireUserId();
    Transcription t = transcriptionService.getOwned(uid, id);
    if (t.getTtsAudioPath() == null) {
        return ResponseEntity.notFound().build();
    }
    FileSystemResource res = new FileSystemResource(t.getTtsAudioPath());
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"summary-" + id + ".mp3\"")
            .body(res);
}
```

> SummaryController 构造函数现在有三个参数：`SummaryService`、`CurrentUser`、`TranscriptionService`。更新构造函数体一并赋值。

- [ ] **Step 4: 给 TranscriptionController 加 Markdown 下载**

在 `TranscriptionController.java` 新增：

```java
// 顶部 import
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;

@GetMapping("/{id}/download")
public ResponseEntity<byte[]> download(@PathVariable Long id) {
    Long uid = currentUser.requireUserId();
    var t = service.getOwned(uid, id);
    String md = t.getSummaryMarkdown() == null ? "" : t.getSummaryMarkdown();
    byte[] bytes = md.getBytes(StandardCharsets.UTF_8);
    String filename = "summary-" + id + ".md";
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/markdown; charset=UTF-8")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(bytes);
}
```

- [ ] **Step 5: 写集成测试**

`backend/src/test/java/com/voicenotes/controller/SpeechAndDownloadIT.java`:

```java
package com.voicenotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicenotes.domain.SummaryTemplate;
import com.voicenotes.domain.Transcription;
import com.voicenotes.domain.TranscriptionStatus;
import com.voicenotes.repository.TranscriptionRepository;
import com.voicenotes.service.TtsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpeechAndDownloadIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TranscriptionRepository repo;
    @MockitoBean TtsService ttsService;

    private String token(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret123\"}";
        String json = mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json).get("token").asText();
    }

    private Long seedDone(Long userId) {
        Transcription t = new Transcription();
        t.setUserId(userId);
        t.setOriginalFilename("a.mp3");
        t.setAudioPath("./build/test-audio/a.mp3");
        t.setStatus(TranscriptionStatus.DONE);
        t.setTemplate(SummaryTemplate.GENERAL);
        t.setSummaryMarkdown("# 概括\n- 要点");
        return repo.save(t).getId();
    }

    @Test
    void generateSpeechThenDownloadMd() throws Exception {
        when(ttsService.synthesize(anyString())).thenReturn(new byte[]{1, 2, 3});
        String tk = token("ttsUser");
        // seed 需要 userId：注册后第一条用户 id=1（H2 自增），简单起见用 list 接口取不便，直接 seed userId=1
        Long id = seedDone(1L);

        mvc.perform(post("/api/summaries/" + id + "/speech")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audioUrl").value("/api/summaries/" + id + "/speech"));

        mvc.perform(get("/api/summaries/" + id + "/speech")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "audio/mpeg"));

        mvc.perform(get("/api/transcriptions/" + id + "/download")
                        .header("Authorization", "Bearer " + tk))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString(".md")));
    }
}
```

> **测试备注**：`seedDone(1L)` 假定注册的首个用户 id 为 1。若 H2 自增起点不同导致越权 403，改为：先调 `GET /api/transcriptions` 确认空，再通过上传接口建记录并用 `repo` 直接补 `summaryMarkdown` 后保存。保持「记录归属当前 token 用户」即可。

- [ ] **Step 6: 运行确认通过 + 全量回归**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/voicenotes backend/src/test/java/com/voicenotes/controller/SpeechAndDownloadIT.java
git commit -m "feat: add TTS generate/play and markdown download endpoints"
```

---

### Task 17: 后端手动冒烟（真实依赖，可选但建议）

**Files:** 无（手动验证）

- [ ] **Step 1: 准备环境变量并启动 MySQL**

设置（PowerShell 示例，值替换为真实密钥）：

```bash
export DASHSCOPE_API_KEY=...        # 你的 DashScope key
export TTS_BASE_URL=...             # 中转站 base url，形如 https://xxx/v1
export TTS_API_KEY=...              # 中转站 key
export JWT_SECRET=$(head -c 48 /dev/urandom | base64)
export DB_USER=root DB_PASSWORD=...
```

确保本地 MySQL 8 运行中（库 `voicenotes` 会自动建）。

- [ ] **Step 2: 启动后端**

Run: `cd backend && ./gradlew bootRun`
Expected: 控制台显示 `Started VoiceNotesApplication`，监听 8080。

- [ ] **Step 3: 用 curl 跑一遍主链路**

```bash
# 注册
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '{"username":"demo","password":"secret123"}' | python -c 'import sys,json;print(json.load(sys.stdin)["token"])')
# 上传（准备一个短录音 sample.mp3）
ID=$(curl -s -X POST localhost:8080/api/transcriptions -H "Authorization: Bearer $TOKEN" -F file=@sample.mp3 -F template=MEETING | python -c 'import sys,json;print(json.load(sys.stdin)["id"])')
# 转写
curl -s -X POST localhost:8080/api/transcriptions/$ID/transcribe -H "Authorization: Bearer $TOKEN"
# 概括（SSE）
curl -N "localhost:8080/api/summaries/$ID/stream?token=$TOKEN"
# 朗读
curl -s -X POST localhost:8080/api/summaries/$ID/speech -H "Authorization: Bearer $TOKEN"
```

Expected: 转写返回 TRANSCRIBED；SSE 持续吐 `event:chunk`；朗读返回 audioUrl。任一步报错则看返回的 `error` 字段排查（多半是 key/配置）。

- [ ] **Step 4: 记录结果（不提交代码，仅确认链路通）**

确认后即可进入前端开发。若 DashScope/TTS 配置未就绪，可跳过本任务，靠单元/集成测试保证逻辑正确。

### Task 18: 前端类型、API 封装与鉴权 store

**Files:**
- Create: `frontend/src/types.ts`
- Create: `frontend/src/store/auth.ts`
- Create: `frontend/src/lib/api.ts`
- Test: `frontend/src/__tests__/api.test.ts`

- [ ] **Step 1: 写共享类型（与后端 DTO 对齐）**

`frontend/src/types.ts`:

```ts
export type TranscriptionStatus =
  | 'UPLOADED' | 'TRANSCRIBING' | 'TRANSCRIBED'
  | 'SUMMARIZING' | 'DONE' | 'FAILED';

export type SummaryTemplate = 'MEETING' | 'LECTURE' | 'GENERAL';

export interface Detail {
  id: number;
  originalFilename: string;
  status: TranscriptionStatus;
  transcriptText: string | null;
  summaryMarkdown: string | null;
  hasTtsAudio: boolean;
  template: SummaryTemplate;
  errorMessage: string | null;
  createdAt: string;
}

export interface ListItem {
  id: number;
  originalFilename: string;
  status: TranscriptionStatus;
  template: SummaryTemplate;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  username: string;
}
```

- [ ] **Step 2: 写鉴权 store（localStorage 持久化）**

`frontend/src/store/auth.ts`:

```ts
const TOKEN_KEY = 'vn_token';
const USER_KEY = 'vn_user';

export const auth = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  getUsername(): string | null {
    return localStorage.getItem(USER_KEY);
  },
  set(token: string, username: string) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, username);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  isLoggedIn(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  },
};
```

- [ ] **Step 3: 写 API 封装（带 JWT，401 自动登出）**

`frontend/src/lib/api.ts`:

```ts
import { auth } from '@/store/auth';
import type { AuthResponse, Detail, ListItem, SummaryTemplate } from '@/types';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = auth.getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(path, { ...init, headers });
  if (res.status === 401) {
    auth.clear();
    throw new ApiError(401, '登录已过期');
  }
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.error) msg = body.error;
    } catch {
      // 忽略非 JSON 响应
    }
    throw new ApiError(res.status, msg);
  }
  const ct = res.headers.get('content-type') ?? '';
  return (ct.includes('application/json') ? await res.json() : (undefined as T));
}

export const api = {
  ApiError,
  register: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  login: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  upload: (file: File, template: SummaryTemplate) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('template', template);
    return request<{ id: number; status: string }>('/api/transcriptions', {
      method: 'POST',
      body: fd,
    });
  },
  transcribe: (id: number) =>
    request<Detail>(`/api/transcriptions/${id}/transcribe`, { method: 'POST' }),
  detail: (id: number) => request<Detail>(`/api/transcriptions/${id}`),
  list: () => request<ListItem[]>('/api/transcriptions'),
  remove: (id: number) =>
    request<void>(`/api/transcriptions/${id}`, { method: 'DELETE' }),
  generateSpeech: (id: number) =>
    request<{ id: number; audioUrl: string }>(`/api/summaries/${id}/speech`, {
      method: 'POST',
    }),
  downloadUrl: (id: number) => `/api/transcriptions/${id}/download`,
  speechUrl: (id: number) => `/api/summaries/${id}/speech`,
};
```

- [ ] **Step 4: 写测试（mock fetch）**

`frontend/src/__tests__/api.test.ts`:

```ts
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';

describe('api', () => {
  beforeEach(() => {
    auth.clear();
    vi.restoreAllMocks();
  });

  it('attaches bearer token', async () => {
    auth.set('tok123', 'alice');
    const spy = vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    );
    await api.list();
    const init = spy.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer tok123');
  });

  it('clears auth and throws on 401', async () => {
    auth.set('tok', 'alice');
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response('', { status: 401 }));
    await expect(api.list()).rejects.toThrow('登录已过期');
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('surfaces server error message', async () => {
    vi.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: '用户名已存在' }), {
        status: 409,
        headers: { 'content-type': 'application/json' },
      }),
    );
    await expect(api.register('a', 'b')).rejects.toThrow('用户名已存在');
  });
});
```

- [ ] **Step 5: 运行测试**

Run: `cd frontend && npm run test -- api`
Expected: 3 passed

- [ ] **Step 6: Commit**

```bash
git add frontend/src/types.ts frontend/src/store frontend/src/lib/api.ts frontend/src/__tests__/api.test.ts
git commit -m "feat: add frontend types, api client, and auth store"
```

---

### Task 19: SSE 概括流接收工具

**Files:**
- Create: `frontend/src/lib/sse.ts`
- Test: `frontend/src/__tests__/sse.test.ts`

- [ ] **Step 1: 写测试（mock EventSource）**

`frontend/src/__tests__/sse.test.ts`:

```ts
import { describe, it, expect, vi } from 'vitest';
import { streamSummary } from '@/lib/sse';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  closed = false;
  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }
  addEventListener(type: string, cb: (e: MessageEvent) => void) {
    (this.listeners[type] ??= []).push(cb);
  }
  emit(type: string, data: string) {
    (this.listeners[type] ?? []).forEach((cb) =>
      cb(new MessageEvent(type, { data })),
    );
  }
  close() {
    this.closed = true;
  }
}

describe('streamSummary', () => {
  it('accumulates chunks and resolves on done', async () => {
    vi.stubGlobal('EventSource', FakeEventSource as unknown as typeof EventSource);
    const chunks: string[] = [];
    const promise = streamSummary(7, 'tok', (c) => chunks.push(c));

    const es = FakeEventSource.instances.at(-1)!;
    expect(es.url).toContain('/api/summaries/7/stream');
    expect(es.url).toContain('token=tok');

    es.emit('chunk', '# 概括\n');
    es.emit('chunk', '- 要点');
    es.emit('done', '[DONE]');

    await promise;
    expect(chunks).toEqual(['# 概括\n', '- 要点']);
    expect(es.closed).toBe(true);
  });

  it('rejects on error event', async () => {
    vi.stubGlobal('EventSource', FakeEventSource as unknown as typeof EventSource);
    const promise = streamSummary(8, 'tok', () => {});
    const es = FakeEventSource.instances.at(-1)!;
    es.emit('error', '概括失败');
    await expect(promise).rejects.toThrow('概括失败');
  });
});
```

- [ ] **Step 2: 运行确认失败**

Run: `cd frontend && npm run test -- sse`
Expected: FAIL（streamSummary 未定义）

- [ ] **Step 3: 实现 sse.ts**

`frontend/src/lib/sse.ts`:

```ts
/**
 * 打开 SSE 概括流。每个 chunk 通过 onChunk 回调；done 时 resolve；error 时 reject。
 * token 通过 query 传递（EventSource 不能设 header）。
 */
export function streamSummary(
  id: number,
  token: string,
  onChunk: (chunk: string) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const url = `/api/summaries/${id}/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);

    es.addEventListener('chunk', (e: MessageEvent) => {
      onChunk(e.data);
    });
    es.addEventListener('done', () => {
      es.close();
      resolve();
    });
    es.addEventListener('error', (e: MessageEvent) => {
      es.close();
      const msg = typeof e.data === 'string' && e.data ? e.data : '生成中断';
      reject(new Error(msg));
    });
  });
}
```

> 说明：原生 `EventSource` 的网络错误事件没有 `data`；上面的 `error` 监听同时覆盖后端主动发的 `event: error`（带消息）与连接断开（用默认「生成中断」）。

- [ ] **Step 4: 运行确认通过**

Run: `cd frontend && npm run test -- sse`
Expected: 2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/lib/sse.ts frontend/src/__tests__/sse.test.ts
git commit -m "feat: add SSE summary stream client"
```

---

### Task 20: shadcn 风格基础组件 + 登录页

**Files:**
- Create: `frontend/src/components/ui/button.tsx`
- Create: `frontend/src/components/ui/card.tsx`
- Create: `frontend/src/components/ui/input.tsx`
- Create: `frontend/src/pages/LoginPage.tsx`
- Modify: `frontend/src/App.tsx`（加路由）
- Test: `frontend/src/__tests__/LoginPage.test.tsx`

- [ ] **Step 1: 写基础 UI 组件**

`frontend/src/components/ui/button.tsx`:

```tsx
import { cn } from '@/lib/utils';
import { ButtonHTMLAttributes, forwardRef } from 'react';

type Variant = 'primary' | 'accent' | 'ghost' | 'destructive';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
}

const styles: Record<Variant, string> = {
  primary: 'bg-primary text-primary-fg hover:opacity-90',
  accent: 'bg-accent text-accent-fg hover:opacity-90',
  ghost: 'bg-transparent hover:bg-muted',
  destructive: 'bg-destructive text-white hover:opacity-90',
};

export const Button = forwardRef<HTMLButtonElement, Props>(
  ({ className, variant = 'primary', disabled, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium',
        'transition-[background-color,opacity,transform] duration-200 active:scale-[0.97]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary',
        'disabled:opacity-50 disabled:pointer-events-none cursor-pointer',
        styles[variant],
        className,
      )}
      {...props}
    />
  ),
);
Button.displayName = 'Button';
```

`frontend/src/components/ui/card.tsx`:

```tsx
import { cn } from '@/lib/utils';
import { HTMLAttributes } from 'react';

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'rounded-xl border border-[#99F6E4]/60 bg-white/80 dark:bg-white/5 dark:border-white/10 p-5',
        className,
      )}
      {...props}
    />
  );
}
```

`frontend/src/components/ui/input.tsx`:

```tsx
import { cn } from '@/lib/utils';
import { InputHTMLAttributes, forwardRef } from 'react';

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        'w-full rounded-lg border border-[#99F6E4] bg-white/90 dark:bg-white/5 px-3 py-2 text-sm',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary',
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = 'Input';
```

- [ ] **Step 2: 写 LoginPage（注册/登录切换）**

`frontend/src/pages/LoginPage.tsx`:

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';

export default function LoginPage() {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const fn = mode === 'login' ? api.login : api.register;
      const res = await fn(username, password);
      auth.set(res.token, res.username);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-dvh flex items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <h1 className="text-2xl font-bold mb-1">VoiceNotes</h1>
        <p className="text-sm opacity-70 mb-6">
          {mode === 'login' ? '登录以继续' : '创建一个新账户'}
        </p>
        <form onSubmit={submit} className="space-y-3">
          <Input
            placeholder="用户名"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />
          <Input
            type="password"
            placeholder="密码"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          />
          {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? '处理中...' : mode === 'login' ? '登录' : '注册'}
          </Button>
        </form>
        <button
          className="mt-4 text-sm text-primary hover:underline cursor-pointer"
          onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null); }}
        >
          {mode === 'login' ? '没有账户？去注册' : '已有账户？去登录'}
        </button>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: 改 App.tsx 加路由与守卫**

`frontend/src/App.tsx`:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import LoginPage from '@/pages/LoginPage';
import WorkbenchPage from '@/pages/WorkbenchPage';
import { auth } from '@/store/auth';

function Protected({ children }: { children: React.ReactNode }) {
  return auth.isLoggedIn() ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-center" richColors />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<Protected><WorkbenchPage /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

> WorkbenchPage 在 Task 25 创建。为让本任务可编译/测试，先建一个占位：`frontend/src/pages/WorkbenchPage.tsx` 内容 `export default function WorkbenchPage(){return <div>Workbench</div>;}`，Task 25 再替换为完整实现。

- [ ] **Step 4: 写 LoginPage 测试**

`frontend/src/__tests__/LoginPage.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import LoginPage from '@/pages/LoginPage';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';

describe('LoginPage', () => {
  beforeEach(() => { auth.clear(); vi.restoreAllMocks(); });

  it('toggles to register mode', async () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await userEvent.click(screen.getByText('没有账户？去注册'));
    expect(screen.getByText('注册')).toBeInTheDocument();
  });

  it('shows error on failed login', async () => {
    vi.spyOn(api, 'login').mockRejectedValue(new Error('用户名或密码错误'));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await userEvent.type(screen.getByPlaceholderText('用户名'), 'a');
    await userEvent.type(screen.getByPlaceholderText('密码'), 'bbbbbb');
    await userEvent.click(screen.getByRole('button', { name: '登录' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('用户名或密码错误');
  });
});
```

- [ ] **Step 5: 运行测试**

Run: `cd frontend && npm run test -- LoginPage`
Expected: 2 passed

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/ui frontend/src/pages/LoginPage.tsx frontend/src/pages/WorkbenchPage.tsx frontend/src/App.tsx frontend/src/__tests__/LoginPage.test.tsx
git commit -m "feat: add base UI components and login page with routing"
```

### Task 21: Dropzone 拖拽上传组件（含校验与动效）

**Files:**
- Create: `frontend/src/components/Dropzone.tsx`
- Test: `frontend/src/__tests__/Dropzone.test.tsx`

- [ ] **Step 1: 写测试（先红）**

`frontend/src/__tests__/Dropzone.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import { Dropzone } from '@/components/Dropzone';

describe('Dropzone', () => {
  it('rejects unsupported type and does not call onFile', async () => {
    const onFile = vi.fn();
    render(<Dropzone onFile={onFile} disabled={false} />);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const bad = new File(['x'], 'notes.txt', { type: 'text/plain' });
    await userEvent.upload(input, bad);
    expect(onFile).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('mp3');
  });

  it('accepts mp3 and calls onFile', async () => {
    const onFile = vi.fn();
    render(<Dropzone onFile={onFile} disabled={false} />);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const good = new File(['x'], 'meeting.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(input, good);
    expect(onFile).toHaveBeenCalledWith(good);
  });
});
```

- [ ] **Step 2: 运行确认失败**

Run: `cd frontend && npm run test -- Dropzone`
Expected: FAIL

- [ ] **Step 3: 实现 Dropzone**

`frontend/src/components/Dropzone.tsx`:

```tsx
import { useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { UploadCloud } from 'lucide-react';
import { cn } from '@/lib/utils';

const ALLOWED = ['mp3', 'wav', 'm4a'];
const MAX_BYTES = 200 * 1024 * 1024;

function validate(file: File): string | null {
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (!ALLOWED.includes(ext)) return '仅支持 mp3/wav/m4a 格式';
  if (file.size > MAX_BYTES) return '文件过大，最大 200MB';
  return null;
}

export function Dropzone({ onFile, disabled }: { onFile: (f: File) => void; disabled: boolean }) {
  const [dragOver, setDragOver] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handle(file: File | undefined) {
    if (!file) return;
    const err = validate(file);
    if (err) { setError(err); return; }
    setError(null);
    onFile(file);
  }

  return (
    <div>
      <motion.div
        animate={{ scale: dragOver ? 1.02 : 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
        onDragOver={(e) => { e.preventDefault(); if (!disabled) setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          if (!disabled) handle(e.dataTransfer.files?.[0]);
        }}
        onClick={() => !disabled && inputRef.current?.click()}
        className={cn(
          'flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed',
          'px-8 py-16 text-center cursor-pointer transition-colors duration-200',
          dragOver ? 'border-accent bg-accent/5' : 'border-[#99F6E4] bg-muted/40',
          disabled && 'opacity-50 pointer-events-none',
        )}
      >
        <motion.div animate={{ y: dragOver ? -4 : 0 }}>
          <UploadCloud className="h-10 w-10 text-primary" />
        </motion.div>
        <p className="text-lg font-medium">拖拽音频到这里，或点击选择文件</p>
        <p className="text-sm opacity-60">支持 mp3 / wav / m4a · 最大 200MB</p>
        <input
          ref={inputRef}
          data-testid="file-input"
          type="file"
          accept=".mp3,.wav,.m4a,audio/*"
          className="hidden"
          onChange={(e) => handle(e.target.files?.[0])}
        />
      </motion.div>
      {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
    </div>
  );
}
```

- [ ] **Step 4: 运行确认通过**

Run: `cd frontend && npm run test -- Dropzone`
Expected: 2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/Dropzone.tsx frontend/src/__tests__/Dropzone.test.tsx
git commit -m "feat: add drag-drop upload component with validation"
```

---

### Task 22: 转写进度组件（骨架屏 + 预估时间）

**Files:**
- Create: `frontend/src/components/TranscribeProgress.tsx`
- Test: `frontend/src/__tests__/TranscribeProgress.test.tsx`

- [ ] **Step 1: 写测试**

`frontend/src/__tests__/TranscribeProgress.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TranscribeProgress } from '@/components/TranscribeProgress';

describe('TranscribeProgress', () => {
  it('shows transcribing label', () => {
    render(<TranscribeProgress />);
    expect(screen.getByText(/转写中/)).toBeInTheDocument();
  });

  it('has aria-live for accessibility', () => {
    render(<TranscribeProgress />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行确认失败**

Run: `cd frontend && npm run test -- TranscribeProgress`
Expected: FAIL

- [ ] **Step 3: 实现 TranscribeProgress**

`frontend/src/components/TranscribeProgress.tsx`:

```tsx
import { motion } from 'framer-motion';

export function TranscribeProgress() {
  return (
    <div role="status" aria-live="polite" className="space-y-4">
      <div className="flex items-center gap-3">
        <motion.span
          className="inline-block h-3 w-3 rounded-full bg-primary"
          animate={{ opacity: [1, 0.3, 1] }}
          transition={{ duration: 1.2, repeat: Infinity }}
        />
        <span className="text-sm font-medium">转写中，请稍候…</span>
      </div>
      {/* 骨架屏 shimmer */}
      <div className="space-y-2">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-4 w-full overflow-hidden rounded bg-muted">
            <motion.div
              className="h-full w-1/3 bg-gradient-to-r from-transparent via-white/60 to-transparent"
              animate={{ x: ['-100%', '300%'] }}
              transition={{ duration: 1.4, repeat: Infinity, delay: i * 0.15 }}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
```

> 预估时间需要音频时长才能估，本组件保持纯展示。若后续要显示「约剩 N 分钟」，由父组件用 `<audio>.duration` 估算后作为 prop 传入；当前范围不强制。

- [ ] **Step 4: 运行确认通过**

Run: `cd frontend && npm run test -- TranscribeProgress`
Expected: 2 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/TranscribeProgress.tsx frontend/src/__tests__/TranscribeProgress.test.tsx
git commit -m "feat: add transcription progress with shimmer skeleton"
```

---

### Task 23: 流式 Markdown 渲染组件（SummaryView）

**Files:**
- Create: `frontend/src/components/SummaryView.tsx`
- Test: `frontend/src/__tests__/SummaryView.test.tsx`

- [ ] **Step 1: 写测试**

`frontend/src/__tests__/SummaryView.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { SummaryView } from '@/components/SummaryView';

describe('SummaryView', () => {
  it('renders markdown headings', () => {
    render(<SummaryView markdown={'# 会议纪要\n\n- 要点一'} streaming={false} />);
    expect(screen.getByRole('heading', { name: '会议纪要' })).toBeInTheDocument();
    expect(screen.getByText('要点一')).toBeInTheDocument();
  });

  it('shows streaming cursor when streaming', () => {
    render(<SummaryView markdown={'# 标题'} streaming={true} />);
    expect(screen.getByTestId('stream-cursor')).toBeInTheDocument();
  });

  it('hides cursor when not streaming', () => {
    render(<SummaryView markdown={'# 标题'} streaming={false} />);
    expect(screen.queryByTestId('stream-cursor')).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行确认失败**

Run: `cd frontend && npm run test -- SummaryView`
Expected: FAIL

- [ ] **Step 3: 实现 SummaryView**

`frontend/src/components/SummaryView.tsx`:

```tsx
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { motion } from 'framer-motion';

export function SummaryView({ markdown, streaming }: { markdown: string; streaming: boolean }) {
  return (
    <div aria-live="polite" className="prose-like leading-relaxed text-[15px]">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{markdown}</ReactMarkdown>
      {streaming && (
        <motion.span
          data-testid="stream-cursor"
          className="inline-block h-4 w-[2px] translate-y-[2px] bg-primary"
          animate={{ opacity: [1, 0, 1] }}
          transition={{ duration: 0.9, repeat: Infinity }}
        />
      )}
    </div>
  );
}
```

> 样式：`.prose-like` 可在 `index.css` 里加基础排版规则（标题间距、列表缩进、`code` 等宽）。本任务不强制美化，渲染正确即可，Task 27 统一打磨样式。

- [ ] **Step 4: 运行确认通过**

Run: `cd frontend && npm run test -- SummaryView`
Expected: 3 passed

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/SummaryView.tsx frontend/src/__tests__/SummaryView.test.tsx
git commit -m "feat: add streaming markdown view with cursor"
```

---

### Task 24: 音频播放器、工具栏、模板选择、主题切换、历史侧栏

**Files:**
- Create: `frontend/src/components/AudioPlayer.tsx`
- Create: `frontend/src/components/Toolbar.tsx`
- Create: `frontend/src/components/TemplateSelect.tsx`
- Create: `frontend/src/components/ThemeToggle.tsx`
- Create: `frontend/src/components/HistorySidebar.tsx`
- Test: `frontend/src/__tests__/HistorySidebar.test.tsx`, `frontend/src/__tests__/Toolbar.test.tsx`

- [ ] **Step 1: 写 AudioPlayer（原生 audio + 下载）**

`frontend/src/components/AudioPlayer.tsx`:

```tsx
import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Download } from 'lucide-react';
import { auth } from '@/store/auth';

/**
 * 播放 TTS 音频。后端 GET /api/summaries/{id}/speech 需带 token；
 * <audio> 不能设 header，这里通过查询参数传 token（JwtAuthFilter 支持）。
 */
export function AudioPlayer({ id }: { id: number }) {
  const token = auth.getToken() ?? '';
  const src = `/api/summaries/${id}/speech?token=${encodeURIComponent(token)}`;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className="mt-4 flex items-center gap-3 rounded-lg border border-[#99F6E4]/60 p-3"
    >
      <audio controls src={src} className="h-9 flex-1" data-testid="tts-audio" />
      <a href={src} download={`summary-${id}.mp3`}>
        <Button variant="ghost" aria-label="下载音频">
          <Download className="h-4 w-4" /> mp3
        </Button>
      </a>
    </motion.div>
  );
}
```

- [ ] **Step 2: 写 Toolbar（下载 md / 复制 / 朗读）**

`frontend/src/components/Toolbar.tsx`:

```tsx
import { Button } from '@/components/ui/button';
import { Download, Copy, Volume2 } from 'lucide-react';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';
import { toast } from 'sonner';

interface Props {
  id: number;
  markdown: string;
  onSpeechReady: () => void;
  speaking: boolean;
}

export function Toolbar({ id, markdown, onSpeechReady, speaking }: Props) {
  async function copy() {
    await navigator.clipboard.writeText(markdown);
    toast.success('已复制到剪贴板');
  }

  function downloadMd() {
    const token = auth.getToken() ?? '';
    const a = document.createElement('a');
    a.href = `${api.downloadUrl(id)}?token=${encodeURIComponent(token)}`;
    a.download = `summary-${id}.md`;
    a.click();
  }

  async function speak() {
    try {
      await api.generateSpeech(id);
      toast.success('朗读音频已生成');
      onSpeechReady();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '朗读生成失败');
    }
  }

  return (
    <div className="flex flex-wrap gap-2">
      <Button variant="accent" onClick={downloadMd}>
        <Download className="h-4 w-4" /> 下载 .md
      </Button>
      <Button variant="ghost" onClick={copy}>
        <Copy className="h-4 w-4" /> 复制
      </Button>
      <Button variant="ghost" onClick={speak} disabled={speaking}>
        <Volume2 className="h-4 w-4" /> {speaking ? '生成中…' : '朗读'}
      </Button>
    </div>
  );
}
```

> `api.downloadUrl` 是后端 `/download` 路径；它受 JWT 保护，浏览器直链下载无法设 header，因此同样附 `?token=`。**注意**：需要后端 `/api/transcriptions/{id}/download` 也接受 query token —— `JwtAuthFilter`（Task 5）对所有请求都支持 `?token=`，故无需改后端。

- [ ] **Step 3: 写 TemplateSelect 与 ThemeToggle**

`frontend/src/components/TemplateSelect.tsx`:

```tsx
import type { SummaryTemplate } from '@/types';

const LABELS: Record<SummaryTemplate, string> = {
  MEETING: '会议纪要',
  LECTURE: '课堂笔记',
  GENERAL: '通用概括',
};

export function TemplateSelect({
  value, onChange, disabled,
}: { value: SummaryTemplate; onChange: (v: SummaryTemplate) => void; disabled?: boolean }) {
  return (
    <select
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value as SummaryTemplate)}
      aria-label="概括模板"
      className="rounded-lg border border-[#99F6E4] bg-white/90 dark:bg-white/5 px-3 py-1.5 text-sm cursor-pointer"
    >
      {(Object.keys(LABELS) as SummaryTemplate[]).map((k) => (
        <option key={k} value={k}>{LABELS[k]}</option>
      ))}
    </select>
  );
}
```

`frontend/src/components/ThemeToggle.tsx`:

```tsx
import { useEffect, useState } from 'react';
import { Moon, Sun } from 'lucide-react';
import { Button } from '@/components/ui/button';

export function ThemeToggle() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'));
  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark);
  }, [dark]);
  return (
    <Button variant="ghost" aria-label="切换主题" onClick={() => setDark((d) => !d)}>
      {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </Button>
  );
}
```

- [ ] **Step 4: 写 HistorySidebar（stagger 动效 + 状态标签 + 删除）**

`frontend/src/components/HistorySidebar.tsx`:

```tsx
import { motion, AnimatePresence } from 'framer-motion';
import { Trash2, Plus } from 'lucide-react';
import type { ListItem, TranscriptionStatus } from '@/types';
import { cn } from '@/lib/utils';

const STATUS_LABEL: Record<TranscriptionStatus, string> = {
  UPLOADED: '待转写', TRANSCRIBING: '转写中', TRANSCRIBED: '待概括',
  SUMMARIZING: '概括中', DONE: '完成', FAILED: '失败',
};

interface Props {
  items: ListItem[];
  activeId: number | null;
  onSelect: (id: number) => void;
  onDelete: (id: number) => void;
  onNew: () => void;
}

export function HistorySidebar({ items, activeId, onSelect, onDelete, onNew }: Props) {
  return (
    <aside className="flex w-60 flex-col gap-2 border-r border-[#99F6E4]/40 p-3">
      <button
        onClick={onNew}
        className="flex items-center justify-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-fg hover:opacity-90 cursor-pointer"
      >
        <Plus className="h-4 w-4" /> 新建
      </button>
      <h2 className="mt-2 px-1 text-xs font-semibold uppercase opacity-50">历史记录</h2>
      <div className="flex-1 space-y-1 overflow-y-auto">
        <AnimatePresence initial={false}>
          {items.map((it, i) => (
            <motion.div
              key={it.id}
              layout
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -8 }}
              transition={{ delay: i * 0.03 }}
              className={cn(
                'group flex items-center justify-between rounded-lg px-3 py-2 text-sm cursor-pointer',
                activeId === it.id ? 'bg-muted' : 'hover:bg-muted/60',
              )}
              onClick={() => onSelect(it.id)}
            >
              <div className="min-w-0">
                <p className="truncate">{it.originalFilename}</p>
                <p className="text-xs opacity-50">{STATUS_LABEL[it.status]}</p>
              </div>
              <button
                aria-label="删除"
                onClick={(e) => { e.stopPropagation(); onDelete(it.id); }}
                className="opacity-0 group-hover:opacity-100 text-destructive cursor-pointer"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </aside>
  );
}
```

- [ ] **Step 5: 写测试（HistorySidebar 与 Toolbar）**

`frontend/src/__tests__/HistorySidebar.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import { HistorySidebar } from '@/components/HistorySidebar';
import type { ListItem } from '@/types';

const items: ListItem[] = [
  { id: 1, originalFilename: 'a.mp3', status: 'DONE', template: 'MEETING', createdAt: '2026-06-03T00:00:00Z' },
  { id: 2, originalFilename: 'b.mp3', status: 'TRANSCRIBING', template: 'GENERAL', createdAt: '2026-06-03T00:00:00Z' },
];

describe('HistorySidebar', () => {
  it('renders items with status labels', () => {
    render(<HistorySidebar items={items} activeId={1} onSelect={() => {}} onDelete={() => {}} onNew={() => {}} />);
    expect(screen.getByText('a.mp3')).toBeInTheDocument();
    expect(screen.getByText('完成')).toBeInTheDocument();
    expect(screen.getByText('转写中')).toBeInTheDocument();
  });

  it('calls onDelete without selecting', async () => {
    const onSelect = vi.fn();
    const onDelete = vi.fn();
    render(<HistorySidebar items={items} activeId={null} onSelect={onSelect} onDelete={onDelete} onNew={() => {}} />);
    await userEvent.click(screen.getAllByLabelText('删除')[0]);
    expect(onDelete).toHaveBeenCalledWith(1);
    expect(onSelect).not.toHaveBeenCalled();
  });
});
```

`frontend/src/__tests__/Toolbar.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Toolbar } from '@/components/Toolbar';
import { api } from '@/lib/api';

describe('Toolbar', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('generates speech and notifies', async () => {
    vi.spyOn(api, 'generateSpeech').mockResolvedValue({ id: 1, audioUrl: '/x' });
    const onReady = vi.fn();
    render(<Toolbar id={1} markdown="# x" onSpeechReady={onReady} speaking={false} />);
    await userEvent.click(screen.getByText('朗读'));
    expect(api.generateSpeech).toHaveBeenCalledWith(1);
    expect(onReady).toHaveBeenCalled();
  });

  it('disables speak button while speaking', () => {
    render(<Toolbar id={1} markdown="# x" onSpeechReady={() => {}} speaking={true} />);
    expect(screen.getByText('生成中…').closest('button')).toBeDisabled();
  });
});
```

- [ ] **Step 6: 运行测试**

Run: `cd frontend && npm run test -- HistorySidebar Toolbar`
Expected: 4 passed

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components frontend/src/__tests__/HistorySidebar.test.tsx frontend/src/__tests__/Toolbar.test.tsx
git commit -m "feat: add audio player, toolbar, template select, theme toggle, history sidebar"
```

### Task 25: 工作台页面（WorkbenchPage — 状态机编排）

> 这是把所有组件接起来的核心页面：管理「当前选中记录 + 流程状态」，驱动 上传→转写→流式概括→朗读 的状态切换，并刷新历史列表。

**Files:**
- Replace: `frontend/src/pages/WorkbenchPage.tsx`（替换 Task 20 的占位实现）
- Test: `frontend/src/__tests__/WorkbenchPage.test.tsx`

- [ ] **Step 1: 实现 WorkbenchPage**

`frontend/src/pages/WorkbenchPage.tsx`:

```tsx
import { useCallback, useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { streamSummary } from '@/lib/sse';
import { auth } from '@/store/auth';
import type { Detail, ListItem, SummaryTemplate } from '@/types';
import { Dropzone } from '@/components/Dropzone';
import { TranscribeProgress } from '@/components/TranscribeProgress';
import { SummaryView } from '@/components/SummaryView';
import { AudioPlayer } from '@/components/AudioPlayer';
import { Toolbar } from '@/components/Toolbar';
import { TemplateSelect } from '@/components/TemplateSelect';
import { ThemeToggle } from '@/components/ThemeToggle';
import { HistorySidebar } from '@/components/HistorySidebar';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

type View = 'idle' | 'transcribing' | 'summarizing' | 'done';

export default function WorkbenchPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ListItem[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [template, setTemplate] = useState<SummaryTemplate>('MEETING');
  const [view, setView] = useState<View>('idle');
  const [markdown, setMarkdown] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [showAudio, setShowAudio] = useState(false);
  const [speaking, setSpeaking] = useState(false);
  const [detail, setDetail] = useState<Detail | null>(null);

  const refreshList = useCallback(async () => {
    try { setItems(await api.list()); } catch { /* 401 已在 api 层处理 */ }
  }, []);

  useEffect(() => { refreshList(); }, [refreshList]);

  function resetToIdle() {
    setActiveId(null); setView('idle'); setMarkdown('');
    setStreaming(false); setShowAudio(false); setDetail(null);
  }

  function logout() { auth.clear(); navigate('/login'); }

  // 主流程：上传 -> 转写 -> 流式概括
  async function handleFile(file: File) {
    try {
      setView('transcribing');
      const up = await api.upload(file, template);
      setActiveId(up.id);
      await refreshList();

      await api.transcribe(up.id); // 后端同步转写，返回即 TRANSCRIBED
      await refreshList();

      await runSummary(up.id);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '处理失败');
      setView('idle');
      refreshList();
    }
  }

  async function runSummary(id: number) {
    setView('summarizing');
    setMarkdown('');
    setStreaming(true);
    setShowAudio(false);
    const token = auth.getToken() ?? '';
    try {
      await streamSummary(id, token, (chunk) => setMarkdown((m) => m + chunk));
      setStreaming(false);
      setView('done');
      await refreshList();
      setDetail(await api.detail(id));
    } catch (e) {
      setStreaming(false);
      toast.error(e instanceof Error ? e.message : '生成中断');
      setView('done'); // 保留已生成部分
    }
  }

  async function openHistory(id: number) {
    try {
      const d = await api.detail(id);
      setActiveId(id);
      setDetail(d);
      setMarkdown(d.summaryMarkdown ?? '');
      setStreaming(false);
      setShowAudio(d.hasTtsAudio);
      if (d.status === 'TRANSCRIBING' || d.status === 'UPLOADED') setView('transcribing');
      else if (d.summaryMarkdown) setView('done');
      else if (d.status === 'TRANSCRIBED' || d.status === 'FAILED') {
        // 已转写未概括：可直接触发概括
        setView('done'); setMarkdown('');
      } else setView('done');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '加载失败');
    }
  }

  async function handleDelete(id: number) {
    try {
      await api.remove(id);
      if (activeId === id) resetToIdle();
      await refreshList();
      toast.success('已删除');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '删除失败');
    }
  }

  async function speakReady() {
    setSpeaking(false);
    setShowAudio(true);
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="flex items-center justify-between border-b border-[#99F6E4]/40 px-4 py-3">
        <span className="text-lg font-bold">VoiceNotes</span>
        <div className="flex items-center gap-2">
          <TemplateSelect value={template} onChange={setTemplate} disabled={view === 'transcribing' || streaming} />
          <ThemeToggle />
          <Button variant="ghost" onClick={logout}>退出</Button>
        </div>
      </header>

      <div className="flex flex-1">
        <HistorySidebar
          items={items}
          activeId={activeId}
          onSelect={openHistory}
          onDelete={handleDelete}
          onNew={resetToIdle}
        />

        <main className="flex-1 p-6">
          <AnimatePresence mode="wait">
            {view === 'idle' && (
              <motion.div key="idle" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                <Dropzone onFile={handleFile} disabled={false} />
              </motion.div>
            )}

            {view === 'transcribing' && (
              <motion.div key="transcribing" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                <Card><TranscribeProgress /></Card>
              </motion.div>
            )}

            {(view === 'summarizing' || view === 'done') && (
              <motion.div key="result" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                <Card>
                  {activeId && markdown && view === 'done' && (
                    <div className="mb-4">
                      <Toolbar id={activeId} markdown={markdown} onSpeechReady={speakReady} speaking={speaking} />
                    </div>
                  )}
                  {markdown
                    ? <SummaryView markdown={markdown} streaming={streaming} />
                    : (
                      <div className="flex flex-col items-start gap-3">
                        <p className="opacity-70">转写完成，尚未概括。</p>
                        {activeId && <Button onClick={() => runSummary(activeId)}>生成概括</Button>}
                      </div>
                    )}
                  {showAudio && activeId && <AudioPlayer id={activeId} />}
                  {detail?.status === 'FAILED' && detail.errorMessage && (
                    <p className="mt-3 text-sm text-destructive" role="alert">{detail.errorMessage}</p>
                  )}
                </Card>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 运行类型检查（确保编译通过）**

Run: `cd frontend && npx tsc -b --noEmit`
Expected: 无错误（若有未用 import 报错，按提示删除）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/WorkbenchPage.tsx
git commit -m "feat: add workbench page orchestrating the full flow"
```

---

### Task 26: 工作台流程集成测试（mock api + sse）

**Files:**
- Test: `frontend/src/__tests__/WorkbenchPage.test.tsx`

- [ ] **Step 1: 写测试**

`frontend/src/__tests__/WorkbenchPage.test.tsx`:

```tsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import WorkbenchPage from '@/pages/WorkbenchPage';
import { api } from '@/lib/api';
import * as sse from '@/lib/sse';
import { auth } from '@/store/auth';

describe('WorkbenchPage flow', () => {
  beforeEach(() => {
    auth.set('tok', 'alice');
    vi.restoreAllMocks();
    vi.spyOn(api, 'list').mockResolvedValue([]);
  });

  it('runs upload -> transcribe -> stream summary', async () => {
    vi.spyOn(api, 'upload').mockResolvedValue({ id: 9, status: 'UPLOADED' });
    vi.spyOn(api, 'transcribe').mockResolvedValue({} as never);
    vi.spyOn(api, 'detail').mockResolvedValue({
      id: 9, originalFilename: 'a.mp3', status: 'DONE',
      transcriptText: 't', summaryMarkdown: '# 概括', hasTtsAudio: false,
      template: 'MEETING', errorMessage: null, createdAt: '2026-06-03T00:00:00Z',
    });
    vi.spyOn(sse, 'streamSummary').mockImplementation(async (_id, _tok, onChunk) => {
      onChunk('# 概括\n');
      onChunk('- 要点');
    });

    render(<MemoryRouter><WorkbenchPage /></MemoryRouter>);

    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const file = new File(['x'], 'a.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(input, file);

    await waitFor(() => expect(sse.streamSummary).toHaveBeenCalled());
    expect(await screen.findByRole('heading', { name: '概括' })).toBeInTheDocument();
    expect(screen.getByText('要点')).toBeInTheDocument();
    // 完成后出现工具栏「朗读」
    expect(await screen.findByText('朗读')).toBeInTheDocument();
  });

  it('shows error toast when transcribe fails', async () => {
    vi.spyOn(api, 'upload').mockResolvedValue({ id: 9, status: 'UPLOADED' });
    vi.spyOn(api, 'transcribe').mockRejectedValue(new Error('转写失败'));

    render(<MemoryRouter><WorkbenchPage /></MemoryRouter>);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    await userEvent.upload(input, new File(['x'], 'a.mp3', { type: 'audio/mpeg' }));

    // 失败后回到 idle，dropzone 文案再次出现
    expect(await screen.findByText(/拖拽音频到这里/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 运行测试**

Run: `cd frontend && npm run test -- WorkbenchPage`
Expected: 2 passed

- [ ] **Step 3: 跑前端全量测试**

Run: `cd frontend && npm run test`
Expected: 全部通过

- [ ] **Step 4: Commit**

```bash
git add frontend/src/__tests__/WorkbenchPage.test.tsx
git commit -m "test: add workbench flow integration tests"
```

---

### Task 27: 排版样式打磨 + 减少动效支持

**Files:**
- Modify: `frontend/src/index.css`

- [ ] **Step 1: 加 Markdown 排版样式与 reduced-motion**

在 `frontend/src/index.css` 末尾追加：

```css
/* Markdown 结果区排版 */
.prose-like h1 { @apply text-2xl font-bold mt-2 mb-3; }
.prose-like h2 { @apply text-xl font-semibold mt-5 mb-2; }
.prose-like h3 { @apply text-lg font-semibold mt-4 mb-2; }
.prose-like p { @apply my-2; }
.prose-like ul { @apply list-disc pl-6 my-2 space-y-1; }
.prose-like ol { @apply list-decimal pl-6 my-2 space-y-1; }
.prose-like code { @apply rounded bg-muted px-1 py-0.5 font-mono text-sm; }
.prose-like pre { @apply rounded-lg bg-muted p-3 overflow-x-auto my-3; }
.prose-like strong { @apply font-semibold; }
.prose-like blockquote { @apply border-l-4 border-primary/40 pl-3 italic opacity-80; }

/* 尊重系统减少动效偏好 */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.001ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.001ms !important;
  }
}
```

- [ ] **Step 2: 跑前端测试确认无回归**

Run: `cd frontend && npm run test`
Expected: 全部通过

- [ ] **Step 3: Commit**

```bash
git add frontend/src/index.css
git commit -m "style: add markdown typography and reduced-motion support"
```

---

### Task 28: 全栈联调与手动验证

**Files:** 无（端到端手动验证）

- [ ] **Step 1: 后端全量测试**

Run: `cd backend && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 前端全量测试 + 构建**

Run: `cd frontend && npm run test && npm run build`
Expected: 测试全过；`dist/` 构建成功

- [ ] **Step 3: 配好密钥启动后端**

设环境变量（值替换为真实密钥，勿提交）：`DASHSCOPE_API_KEY`、`TTS_BASE_URL`、`TTS_API_KEY`、`JWT_SECRET`、`DB_USER`、`DB_PASSWORD`。
Run: `cd backend && ./gradlew bootRun`
Expected: `Started VoiceNotesApplication`，MySQL 库 `voicenotes` 自动建表。

- [ ] **Step 4: 启动前端 dev server**

Run（另开终端）: `cd frontend && npm run dev`
打开浏览器 `http://localhost:5173`。

- [ ] **Step 5: 浏览器手动走查（golden path + 边界）**

逐项确认：
1. 注册新账号 → 自动进入工作台。
2. 拖入一个短录音（mp3，1–2 分钟）→ 看到「转写中」骨架屏动效。
3. 转写完成 → 概括 Markdown 逐块流式出现 + 光标闪烁。
4. 完成后点「下载 .md」→ 得到 `.md` 文件内容正确。
5. 点「复制」→ toast 提示，剪贴板有内容。
6. 点「朗读」→ 按钮转「生成中…」→ 出现音频播放器 → 可播放、可下载 mp3。
7. 左侧历史出现该条；点另一条历史可重新打开查看。
8. 删除一条 → 列表项滑出消失。
9. 切换暗色 → 配色平滑过渡、对比度正常。
10. 拖入 `.txt` → 红色错误提示「仅支持 mp3/wav/m4a」。
11. 退出登录 → 回到登录页；刷新后受保护页跳登录。

- [ ] **Step 6: 记录结果**

若某步失败，按返回的 `error`/控制台日志定位（多为 DashScope/TTS 配置或 SDK 方法签名差异，见 Task 9/12 的适配说明）。逐一修复后重跑 Step 5。

- [ ] **Step 7: 最终提交（如有联调修复）**

```bash
git add -A
git commit -m "chore: full-stack integration fixes after manual verification"
```

---

## 附：环境与密钥清单（实现者必读）

后端需要的环境变量（**绝不写进代码或提交到 git**）：

| 变量 | 用途 |
|---|---|
| `DASHSCOPE_API_KEY` | DashScope（ASR + Qwen） |
| `TTS_BASE_URL` | speech-2.8-hd 中转站 base url（形如 `https://xxx/v1`） |
| `TTS_API_KEY` | TTS 中转站 key |
| `JWT_SECRET` | JWT 签名密钥（≥32 字节随机串） |
| `DB_USER` / `DB_PASSWORD` | MySQL 凭据 |

建议在 `backend/` 下放一个 `.env.example`（仅含变量名、不含值），并把真实 `.env` 加入 `.gitignore`。
