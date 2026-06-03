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
