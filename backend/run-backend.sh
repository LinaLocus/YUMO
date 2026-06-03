#!/usr/bin/env bash
# 启动 VoiceNotes 后端：读取 backend/.env，校验必填项，必要时自动生成 JWT_SECRET，再启动 Spring Boot。
# 用法（在仓库根目录或 backend/ 下均可）：  bash backend/run-backend.sh
set -euo pipefail

# 定位到本脚本所在的 backend 目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f .env ]]; then
  echo "[错误] 未找到 backend/.env。请先复制 .env.example 为 .env 并填值。" >&2
  exit 1
fi

# 载入 .env（忽略注释与空行）
set -a
# shellcheck disable=SC1091
source ./.env
set +a

# 校验必填项
missing=()
[[ -z "${DASHSCOPE_API_KEY:-}" ]] && missing+=("DASHSCOPE_API_KEY")
[[ -z "${DB_PASSWORD:-}" ]] && missing+=("DB_PASSWORD（MySQL 密码，若确为空请填一个空格占位或改用 H2）")
if [[ ${#missing[@]} -gt 0 ]]; then
  echo "[错误] 以下必填项尚未在 backend/.env 中填写：" >&2
  printf '  - %s\n' "${missing[@]}" >&2
  echo "（TTS_BASE_URL / TTS_API_KEY 不填则朗读功能不可用，但转写+概括仍可跑）" >&2
  exit 1
fi

# JWT_SECRET 为空则自动生成一个 48 字节随机串
if [[ -z "${JWT_SECRET:-}" ]]; then
  if command -v openssl >/dev/null 2>&1; then
    export JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  else
    export JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
  fi
  echo "[提示] 已自动生成临时 JWT_SECRET（本次运行有效）。"
fi

echo "[启动] DashScope=已配置  TTS=${TTS_BASE_URL:-未配置}  DB_USER=${DB_USER:-root}"
echo "[启动] 正在启动后端，监听 http://localhost:8080 …（首次启动较慢）"

# 用 gradle wrapper 启动；若无可执行权限则用 sh
if [[ -x ./gradlew ]]; then
  ./gradlew bootRun
else
  sh gradlew bootRun
fi
