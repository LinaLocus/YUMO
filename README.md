# 语墨 EchoInk — 语音转 Markdown 概括系统

导入或拖入语音录音（会议、课程、讲座等），系统自动完成**语音转文字**，再用大模型**流式输出 Markdown 概括**，支持**复制 / 下载 .md**、**TTS 朗读成音频**、**历史记录**与**多用户登录**。形成「音频 → 文字概括 → 再朗读」的闭环。

> Java 课程大作业。后端 Spring Boot（MVC 分层）+ MySQL + JWT，前端 React + Vite + Tailwind + shadcn 风格 + Framer Motion 动效。

---

## 功能特性

- **拖拽上传音频**（mp3 / wav / m4a，≤200MB），带格式与大小校验
- **语音转写**：调用阿里云 DashScope（Paraformer），自动用 ffmpeg 统一转码为 16kHz 单声道
- **流式概括**：Qwen 大模型逐 token 生成，经 SSE 实时推送，前端边收边渲染 Markdown，光标呼吸动效
- **三种概括模板**：会议纪要 / 课堂笔记 / 通用概括（含关键词与待办提取）
- **TTS 朗读**：MiniMax `speech-02-hd` 把概括结果合成音频，在线播放并可下载 mp3
- **历史记录**：MySQL 持久化，可重新查看、删除
- **用户系统**：注册 / 登录（JWT），数据按用户隔离
- **现代 UI**：扁平风、亮/暗色、骨架屏 shimmer、列表 stagger 动效，遵守 `prefers-reduced-motion`

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 21、Spring Boot 3.3（Spring MVC）、Gradle |
| 持久化 | Spring Data JPA、MySQL 8 |
| 鉴权 | Spring Security + JWT（jjwt） |
| 语音/大模型 | DashScope SDK（Paraformer 实时识别 + Qwen 流式） |
| 朗读 | MiniMax t2a_v2（OpenAI 兼容中转站） |
| 流式推送 | Spring MVC `SseEmitter` + JDK 21 虚拟线程 |
| 音频转码 | ffmpeg（外部依赖） |
| 前端 | React 18、Vite、TypeScript |
| 样式/动效 | Tailwind CSS、shadcn 风格组件、Framer Motion |
| Markdown | react-markdown + remark-gfm |
| 测试 | 后端 JUnit5 + Mockito + H2；前端 Vitest + Testing Library |

---

## 架构

```
┌──────────────────────┐   HTTP/SSE   ┌────────────────────────────────┐   SDK/HTTP   ┌──────────────┐
│  React + Vite        │◄────────────►│   Spring Boot (MVC) + Gradle    │◄────────────►│  DashScope   │
│  + Tailwind/shadcn   │              │  Controller → Service → Repo    │              │ (ASR + Qwen) │
│  + Framer Motion     │              │                                 │              ├──────────────┤
└──────────────────────┘              └───────────────┬─────────────────┘              │ MiniMax t2a  │
                                                       │ JPA                            │   (TTS)      │
                                                 ┌─────▼─────┐                          └──────────────┘
                                                 │   MySQL    │
                                                 └───────────┘
```

分层职责：`Controller`（接收请求 / SSE / 鉴权 / DTO）→ `Service`（业务编排、调外部 API、状态流转）→ `Repository`（JPA 持久化）。

**处理流程（状态机）**：`UPLOADED → TRANSCRIBING → TRANSCRIBED → SUMMARIZING → DONE`（任一步失败置 `FAILED` 并记录原因，可从断点重试）。转写为同步批量、概括为流式、朗读为按需可选。

---

## 环境要求

- JDK 21（使用虚拟线程，需 21+）
- Node 18+
- MySQL 8（库 `voicenotes` 会自动创建）
- **ffmpeg**（音频转码必需，需在 PATH 中）
- 一个 DashScope API Key（转写 + 概括）
- 一个 MiniMax 兼容 TTS 中转站的 base URL + key（朗读，可选）

---

## 快速开始

### 1. 配置后端环境变量

复制 `backend/.env.example` 为 `backend/.env`，填入真实值：

```bash
DASHSCOPE_API_KEY=sk-你的key        # 转写+概括
TTS_BASE_URL=https://yunwu.ai       # TTS 中转站（朗读，可选）
TTS_API_KEY=sk-你的key
TTS_MODEL=speech-02-hd
TTS_VOICE=male-qn-qingse
DB_USER=root
DB_PASSWORD=你的MySQL密码
# JWT_SECRET 留空则启动脚本自动生成
```

> `.env` 已被 `.gitignore` 排除，密钥不会进仓库。**切勿提交真实密钥。**

### 2. 启动后端（端口 8080）

```bash
bash backend/run-backend.sh
```

脚本会读取 `.env`、校验必填项、自动生成 JWT 密钥，再启动 Spring Boot。

### 3. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

### 4. 使用

浏览器打开 **http://localhost:5173** → 注册账号 → 选模板 → 拖入短录音 → 观察转写与流式概括 → 下载 / 复制 / 朗读。

---

## 测试

```bash
# 后端（H2 内存库，mock 掉外部 API，无需密钥）
cd backend && ./gradlew test

# 前端
cd frontend && npm run test
```

后端 40 个测试、前端 21 个测试。外部 AI 调用在单元测试中被隔离的 `protected` 方法覆盖，不消耗额度。

---

## REST API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | 注册，返回 JWT |
| POST | `/api/auth/login` | 登录，返回 JWT |
| POST | `/api/transcriptions` | 上传音频（multipart），建记录 |
| POST | `/api/transcriptions/{id}/transcribe` | 触发转写 |
| GET | `/api/transcriptions/{id}` | 查单条状态/结果 |
| GET | `/api/transcriptions` | 当前用户历史列表 |
| GET | `/api/summaries/{id}/stream` | **SSE** 流式概括 |
| POST | `/api/summaries/{id}/speech` | 生成 TTS 朗读音频 |
| GET | `/api/summaries/{id}/speech` | 播放朗读音频 |
| GET | `/api/transcriptions/{id}/download` | 下载 `.md` |
| DELETE | `/api/transcriptions/{id}` | 删除记录 |

除 `/api/auth/**` 外均需带 JWT（`Authorization: Bearer <token>`；SSE 与音频/下载支持 `?token=` 查询参数）。

---

## 目录结构

```
backend/   Spring Boot 后端（config / domain / repository / dto / security / service / controller / exception）
frontend/  React + Vite 前端（components / pages / lib / store）
docs/superpowers/  设计文档（spec）与实现计划（plan）
```

---

## 说明

- 转写采用 DashScope 实时识别喂本地文件（本地部署无需公网 OSS）；耗时约等于录音时长，短录音演示最佳。
- 长概括文本朗读时按句切分、分段合成后拼接。
- 项目为课程作业，生产部署前建议：替换默认 JWT 密钥、改用数据库迁移工具、收紧 CORS。
