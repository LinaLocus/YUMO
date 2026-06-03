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

    /**
     * 按 maxChars 切分，优先在句末标点（。！？!?；;）或换行处断句，
     * 避免把一句话从中间切开导致朗读不自然。单句若本身超过 maxChars 才硬切。
     */
    public List<String> splitText(String text) {
        int max = props.getTts().getMaxChars();
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            cur.append(c);
            boolean isBreak = c == '。' || c == '！' || c == '？' || c == '!' || c == '?'
                    || c == '；' || c == ';' || c == '\n';
            if (cur.length() >= max) {
                // 已达上限：在标点处断，否则硬切
                parts.add(cur.toString());
                cur.setLength(0);
            } else if (isBreak && cur.length() >= max / 2) {
                // 到达句末且已有一定长度，自然断句
                parts.add(cur.toString());
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) parts.add(cur.toString());
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
        // MiniMax 同步语音合成 V2：POST {base}/minimax/v1/t2a_v2
        // 请求体 {model, text, voice_setting:{voice_id}}；响应 {data:{audio: hex 字符串}}。
        String url = props.getTts().getBaseUrl().replaceAll("/+$", "") + "/minimax/v1/t2a_v2";
        String json = "{"
                + "\"model\":\"" + props.getTts().getModel() + "\","
                + "\"text\":" + jsonString(text) + ","
                + "\"voice_setting\":{\"voice_id\":\"" + props.getTts().getVoice() + "\"}"
                + "}";
        Request req = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + props.getTts().getApiKey())
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "TTS 服务返回 " + resp.code());
            }
            String body = resp.body().string();
            return parseAudioHex(body);
        }
    }

    /** 从 MiniMax 响应 JSON 中取 data.audio 的 hex 字符串并解码为字节。 */
    protected byte[] parseAudioHex(String responseJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
            String hex = root.path("data").path("audio").asText("");
            if (hex.isBlank()) {
                String errMsg = root.path("base_resp").path("status_msg").asText("");
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "TTS 返回无音频数据" + (errMsg.isBlank() ? "" : "：" + errMsg));
            }
            int len = hex.length();
            byte[] out = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
            }
            return out;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "解析 TTS 响应失败: " + e.getMessage());
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
