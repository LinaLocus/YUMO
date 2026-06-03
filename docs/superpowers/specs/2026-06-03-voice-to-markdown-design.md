# 语音转 Markdown 概括系统 — 设计文档

- 日期：2026-06-03
- 状态：已确认，待进入实现计划
- 项目代号：VoiceNotes

## 1. 目标

用户导入或拖入语音录音（会议、课程、讲座等），系统自动完成语音转文字，再用大模型流式输出一份 Markdown 概括文档，用户可实时查看、复制和下载 `.md` 文件，并保留历史记录供日后查看。此外，用户可把概括结果用 TTS 朗读成音频，在线播放或下载，形成「音频 → 文字概括 → 再朗读」的闭环。

核心价值：把长录音快速转成结构化、可读、可下载、可听的纪要/笔记。

## 2. 技术栈（已确认）

| 层 | 技术 |
|---|---|
| 后端 | Java + Spring Boot（Spring MVC 同步模型）+ Gradle |
| 持久化 | Spring Data JPA + MySQL |
| 鉴权 | Spring Security + JWT |
| AI 能力（ASR + 概括） | 阿里云 DashScope（`dashscope-sdk-java`）：Paraformer 录音文件识别（ASR）+ Qwen 大模型（概括，流式） |
| AI 能力（TTS 朗读） | speech-2.8-hd（OpenAI 兼容 TTS，经中转站 base URL）：把概括结果朗读成音频 |
| 流式推送 | Spring MVC `SseEmitter`（Server-Sent Events） |
| 前端 | React + Vite |
| 样式 | Tailwind CSS + shadcn/ui，Flat Design 风格，支持亮/暗色 |
| 动效 | Framer Motion + Tailwind transition |
| Markdown 渲染 | react-markdown |

说明：开发期如需零配置启动，可临时切 H2，默认目标数据库为 MySQL。

## 3. 整体架构

```
┌──────────────────────┐         ┌────────────────────────────────┐         ┌──────────────┐
│  React + Vite        │  HTTP   │   Spring Boot (MVC) + Gradle    │   SDK   │  DashScope   │
│  + shadcn/ui +Tailwind│◄──────►│  Controller→Service→Repository  │◄──────►│  Paraformer  │
│      (前端)          │   SSE   │                                 │         │   + Qwen     │
└──────────────────────┘         └───────────────┬────┬───────────┘         └──────────────┘
                                                  │JPA │ HTTP                ┌──────────────┐
                                            ┌─────▼──┐ └────────────────────►│ speech-2.8-hd │
                                            │ MySQL  │                       │ (TTS 中转站)   │
                                            └────────┘                       └──────────────┘
```

分层职责（标准 Spring MVC）：

```
Controller (接收请求/SSE, 鉴权, DTO)
   → Service (业务编排: 调 DashScope, 状态流转)
      → Repository (JPA/MySQL 持久化)
```

## 4. 核心数据流（一次完整处理）

分三步，每步独立接口，前端分别显示进度，后端每步可独立测试、失败可单独重试。

1. **上传**：用户拖入音频 → 前端 `POST /api/transcriptions`（multipart）→ 后端存文件，创建 `Transcription` 记录，状态 `UPLOADED`，返回 `id`。
2. **转写（批量，非流式）**：前端调 `POST /api/transcriptions/{id}/transcribe` → 后端调 DashScope **录音文件识别（异步批量）**，轮询直到拿到全文，状态 `TRANSCRIBING → TRANSCRIBED`。转写阶段前端显示进度条 + 预估剩余时间。
3. **概括（流式）**：前端打开 SSE 连接 `GET /api/summaries/{id}/stream` → 后端把转写全文 + 选定模板喂给 **Qwen 流式接口**，Qwen 吐 token、后端 `SseEmitter` 推一段，前端边收边渲染 Markdown。状态 `SUMMARIZING → DONE`，结果入库。
4. **朗读（可选，按需触发）**：概括完成后，用户点「朗读」→ 前端调 `POST /api/summaries/{id}/speech` → 后端把概括文本（去除 Markdown 标记后的纯文本）发给 speech-2.8-hd，生成整段音频文件存服务器（路径记 `tts_audio_path`），返回音频 URL。前端用 `<audio>` 播放器播放，可暂停/拖动/下载 mp3。再次请求直接复用已生成的音频，不重复调用。

### 4.1 为什么转写用批量而非流式

DashScope 转写有两种模式：

- **批量（录音文件识别）**：整段音频一次性处理，耗时约音频时长的 1/5~1/10，无流式。
- **实时流式识别**：边读边出字，像字幕，但基本按音频原速处理（1 小时录音约需 1 小时）。

对会议/课程长录音，流式 ASR 的「总时长 = 录音时长」是硬伤。因此**转写用批量（整体最快）**，「流式」体验落在概括这一步——这是符合 API 能力、对长录音最划算的设计。转写阶段的等待用进度条 + 预估时间缓解。

> **实现约束（本地部署）**：DashScope 的「批量录音文件识别」要求音频是公网可访问的 URL（OSS/公开链接），本地应用的音频存在本机、DashScope 服务器够不到。因此本地部署采用 **DashScope 实时识别 SDK（`Recognition`）喂本地音频文件**：后端把本地文件流式喂进识别接口、等转写完成后拿全文返回。对用户而言仍是「后端转完返回全文」，体验不变；代价是长录音转写耗时接近录音时长（短录音演示无影响）。若日后部署到公网并配置 OSS，可切换为批量识别以提速。

### 4.2 性能预期（粗估，随网络/负载波动）

| 录音时长 | 总耗时 | 用户体感 |
|---|---|---|
| 5 分钟 | ~1 分钟 | 转写等一会儿，概括秒出 |
| 30 分钟 | ~4–6 分钟 | 转写要等几分钟，概括流畅 |
| 1 小时 | ~8–12 分钟 | 转写明显要等，概括流式书写 |

瓶颈在转写，不在概括。概括首字通常 1–3 秒内开始流式输出。

## 5. 功能范围

「核心 + 历史 + 进阶」三档：

- **核心**：上传/拖入音频 → 转写 → 流式概括 Markdown → 复制/下载 `.md`。
- **历史**：MySQL 存储过往转写与概括结果，列表查看、重新打开、删除。
- **进阶**：
  - 用户注册/登录（JWT），数据按用户隔离。
  - 多种概括模板：会议纪要 / 课堂笔记 / 通用概括（影响喂给 Qwen 的 prompt）。
  - 关键词 / 待办（action items）提取（作为概括模板的一部分输出）。
  - **TTS 朗读**：把概括结果用 speech-2.8-hd 朗读成音频，在线播放（`<audio>`，可暂停/拖动）并可下载 mp3。

## 6. 数据模型（MySQL, JPA）

```
User                          Transcription (核心表)
─────                         ──────────────────────────
id (PK)                       id (PK)
username (唯一)               user_id (FK → User)
password_hash                 original_filename
created_at                    audio_path           (服务器存的音频文件路径)
                              status               (枚举: UPLOADED / TRANSCRIBING /
                                                    TRANSCRIBED / SUMMARIZING / DONE / FAILED)
                              transcript_text       (LONGTEXT, 转写全文)
                              summary_markdown      (LONGTEXT, 概括结果)
                              tts_audio_path        (TTS 朗读音频文件路径, 可空)
                              template              (枚举: MEETING / LECTURE / GENERAL)
                              error_message         (失败原因, 可空)
                              created_at / updated_at
```

- 关系：一个 `User` 一对多 `Transcription`。
- `status` 是整个流程的状态机核心，也是失败恢复的依据。

## 7. REST API

| 方法 | 路径 | 作用 |
|---|---|---|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| POST | `/api/transcriptions` | 上传音频（multipart），建记录，返回 id |
| POST | `/api/transcriptions/{id}/transcribe` | 触发批量转写，后端轮询 DashScope |
| GET | `/api/transcriptions/{id}` | 查单条状态/结果（前端轮询转写进度用） |
| GET | `/api/summaries/{id}/stream` | **SSE**：流式推送 Qwen 概括 Markdown |
| POST | `/api/summaries/{id}/speech` | 触发 TTS 朗读，生成音频文件，返回音频 URL（已生成则复用） |
| GET | `/api/summaries/{id}/speech` | 获取/播放朗读音频文件（`<audio>` src） |
| GET | `/api/transcriptions` | 当前用户的历史列表 |
| GET | `/api/transcriptions/{id}/download` | 下载 `.md` 文件 |
| DELETE | `/api/transcriptions/{id}` | 删除记录（同时删音频文件与 TTS 音频） |

鉴权：除 `/api/auth/**` 外都需带 JWT。音频与概括只能本人访问（Service 层校验 `user_id`）。

### Service 层划分

- `DashScopeService`：封装 ASR 轮询 + Qwen 流式两个 SDK 调用。
- `TtsService`：封装 speech-2.8-hd 调用（OpenAI 兼容 `/v1/audio/speech`，经中转站 base URL），文本转音频文件。
- `TranscriptionService`：状态流转、业务编排。
- `AuthService`：注册/登录、JWT 签发与校验。

## 8. 前端 UI

### 8.1 风格与设计 token

- 风格：shadcn/ui + Tailwind，Flat Design，支持亮/暗色。
- 配色（语义化 token，WCAG 达标）：
  - 主色 Teal `#0D9488`（焦点、链接、主按钮）
  - 强调/CTA 橙色 `#EA580C`（开始处理、下载等关键动作）
  - 背景浅青 `#F0FDFA` / 前景深青 `#134E4A`
  - 危险色 `#DC2626`（删除）
- 字体：Plus Jakarta Sans（标题 + 正文统一）。Markdown 结果区正文行高 1.6，代码块等宽体。

### 8.2 布局（工作台式）

```
┌────────────────────────────────────────────────────────────┐
│  VoiceNotes              [模板▾] [暗色]        [用户名 ▾]      │  顶栏
├──────────────┬─────────────────────────────────────────────┤
│  历史记录     │   ┌────────────────────────────────────┐    │
│ ┌──────────┐ │   │      拖拽音频到这里                  │    │
│ │会议-周报  │ │   │      或点击选择文件                  │    │
│ │ 完成     │ │   │   支持 mp3/wav/m4a · 最大 200MB     │    │
│ ├──────────┤ │   └────────────────────────────────────┘    │
│ │课程-第3章 │ │   ── 处理后这里变成 ──                       │
│ │ 转写中    │ │   ┌────────────────────────────────────┐    │
│ ├──────────┤ │   │ [转写中 ▓▓▓▓░░░░ 约剩 2分钟]         │    │
│ │讲座-AI   │ │   │ # 会议纪要      [下载.md][复制][朗读] │    │
│ │ 完成     │ │   │ ## 核心结论                          │    │
│ └──────────┘ │   │ - 流式逐字蹦出...▌                   │    │
│  [+ 新建]    │   │ ▶ ──●────── 0:42/3:15      [下载mp3] │    │
│              │   └────────────────────────────────────┘    │
└──────────────┴─────────────────────────────────────────────┘
```

### 8.3 三个核心交互状态（主区域随流程切换）

1. **空闲** → 大号拖拽区，拖入文件高亮边框。
2. **转写中** → 进度条 + 预估剩余时间（按音频时长估算），骨架屏占位。
3. **概括中/完成** → Markdown 实时渲染，光标 `▌` 跟随流式内容；完成后顶部出现「下载 .md」「复制」「朗读」。点「朗读」生成音频后，结果区底部出现 `<audio>` 播放器（播放/暂停、进度拖动、下载 mp3）；生成中按钮显示 loading。

### 8.4 关键 UX 细节

- 拖拽区明确视觉反馈；状态切换 150–300ms 过渡。
- 流式文本用 `aria-live` 让无障碍可读。
- 暗色模式独立配色（非简单反色）。
- 响应式：窄屏时历史侧栏收起为抽屉。

### 8.5 技术

React + Vite；`react-markdown` 渲染；原生 `EventSource` 或 `@microsoft/fetch-event-source` 接 SSE；Tailwind + shadcn 组件（Button / Card / Progress / Dialog / Sonner toast）。

## 9. 动态效果

技术：Framer Motion + Tailwind transition。所有动效遵守 `prefers-reduced-motion`，关闭时自动降级。

| 场景 | 动效 | 作用 |
|---|---|---|
| 拖拽音频 | dropzone 边框放大、背景渐变、图标 spring 弹跳 | 提示「可松手」 |
| 状态切换 | fade + 轻微上移 crossfade | 空间连续性 |
| 转写进度 | 进度条平滑推进 + 骨架屏 shimmer | 让等待有动静 |
| 流式输出 | Markdown 逐块淡入，光标 `▌` 呼吸闪烁 | 强化「实时书写」 |
| 历史列表 | 新建项 stagger 滑入（错开 30–50ms），删除滑出 | 列表变化自然 |
| 朗读播放器 | 出现时滑入展开，播放时进度条平滑推进 | 提示音频就绪 |
| 按钮/卡片 | hover 微缩放、press scale 0.97 回弹 | 触感反馈 |
| 完成提示 | Sonner toast + 勾选图标轻弹 | 确认成功 |
| 暗色切换 | 主题色 150–200ms 平滑过渡 | 减少视觉冲击 |

动效原则：

- 时长 150–300ms；进入 ease-out、退出 ease-in 且更快（约 60–70%）。
- 只动 `transform` 和 `opacity`，不动 width/height，避免重排。
- 动效可被打断，用户操作立即响应，不阻塞输入。
- 流式输出是主角动效，其余克制，一屏最多 1–2 个重点动画。

展示亮点：流式 Markdown 逐块淡入 + 光标呼吸；转写阶段 shimmer 骨架屏。从上传到出结果全程有反馈，无「死屏」时刻。

## 10. 错误处理与边界情况

| 失败点 | 处理方式 | 用户看到 |
|---|---|---|
| 上传 — 文件过大/格式不对 | 前端先校验（类型、大小≤200MB），后端二次校验 | 拖拽区红色提示「仅支持 mp3/wav/m4a，最大 200MB」 |
| 上传 — 网络中断 | 失败可重试 | Toast「上传失败，点击重试」 |
| 转写 — DashScope 报错/超时 | 状态置 `FAILED`，存 `error_message` | 卡片显示错误 + 「重新转写」 |
| 转写 — 轮询超时 | 设最大轮询时长（如音频时长×0.5 封顶），超时标失败 | 「转写耗时异常，请重试或换文件」 |
| 概括 — SSE 流中断 | 后端 `SseEmitter` 捕获异常发 error 事件并 complete；已生成部分入库 | 流停止，「生成中断，点击继续」，保留已有内容 |
| 概括 — Qwen 返回空/报错 | 状态 `FAILED` | 「概括失败，重新生成」 |
| 朗读 — TTS 中转站报错/超时 | 不改主状态（朗读是可选附加），返回错误 | Toast「朗读生成失败，点击重试」，已有概括不受影响 |
| 朗读 — 文本过长超 TTS 限制 | 后端按段切分文本、分段合成后拼接音频 | 正常播放完整音频 |
| 鉴权 — token 过期/无效 | 401 拦截 | 跳登录页，「登录已过期」 |
| 越权 — 访问他人记录 | Service 层校验 user_id，403 | 「无权访问」 |

关键设计点：

- **状态机是恢复核心**：失败时停在对应状态 + `error_message`，用户从断点重试，不必从头再来。
- **音频文件**：上传的原始音频与 TTS 生成的朗读音频都存服务器本地目录（`audio_path` / `tts_audio_path`）。默认保留以便重试与复用；删除记录时一并删除两者。
- **TTS 朗读是可选附加功能**：失败不影响已生成的文字概括，主流程状态机不因朗读失败而回退。
- **密钥管理**：DashScope key 与 TTS 中转站的 base URL + key 都放后端环境变量/配置文件，绝不进前端、不进 git。启动时校验是否配置；额度/鉴权错误透传友好提示。

## 11. 测试策略

后端（JUnit 5 + Spring Boot Test + Mockito）：

- **单元测试**：Service 层为主——`TranscriptionService` 状态流转、`AuthService` JWT 签发/校验、模板拼接、`TtsService` 文本切分/拼接逻辑。**Mock 掉 DashScope SDK 和 TTS HTTP 调用**（不真调云端，用假响应），保证测试快、不花钱、可重复。
- **集成测试**：`@SpringBootTest` + `@AutoConfigureMockMvc`，测 REST 接口鉴权、参数校验、状态码；Repository 测试用 H2 内存库。
- **SSE 测试**：验证流式端点正确推送分段数据和结束事件。

前端（Vitest + React Testing Library）：

- 组件测试：dropzone 文件校验、状态切换渲染、Markdown 渲染。
- mock 的 SSE/fetch 验证流式接收和错误处理 UI。

端到端（可选加分）：

- Playwright 跑主流程——上传示例音频 → 转写完成 → 概括流式出现 → 下载。

务实取舍：重点保证 Service 层单元测试 + API 集成测试覆盖核心路径（Java 课程最看重、最好讲解）。E2E 锦上添花。

## 12. 答辩技术亮点

multipart 文件上传、第三方 SDK 集成、异步任务轮询、SSE 流式推送、TTS 语音合成（OpenAI 兼容接口集成）、状态机驱动的失败恢复、JPA 持久化、Spring Security + JWT 鉴权、React 流式渲染与音频播放、Framer Motion 动效。
