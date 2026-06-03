package com.voicenotes.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.voicenotes.config.AppProperties;
import com.voicenotes.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.function.Consumer;

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
        // DashScope 实时识别要求 sampleRate 与音频实际一致，且 paraformer 模型偏好 16kHz 单声道。
        // 用户上传的录音采样率五花八门（44100/48000…），故先用 ffmpeg 统一转码成 16kHz 单声道 wav，
        // 再喂给识别接口，避免 AUDIO_DECODE_ERROR。转写完删除临时文件。
        File converted = transcode16kWav(audioPath);
        try {
            Recognition recognizer = new Recognition();
            RecognitionParam param = RecognitionParam.builder()
                    .model(props.getDashscope().getAsrModel())
                    .format("wav")
                    .sampleRate(16000)
                    .apiKey(props.getDashscope().getApiKey())
                    .build();
            String result = recognizer.call(param, converted);
            if (result == null || result.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "转写返回空结果");
            }
            return extractPlainText(result);
        } finally {
            converted.delete();
        }
    }

    /**
     * DashScope 实时识别返回结构化 JSON（含 sentences/words/时间戳），
     * 这里提取所有 sentence 的 text 拼成纯文本，供概括使用。
     * 若返回的本就是纯文本（非 JSON），原样返回。
     */
    protected String extractPlainText(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed; // 已是纯文本
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
            com.fasterxml.jackson.databind.JsonNode sentences = root.path("sentences");
            if (sentences.isArray() && !sentences.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (com.fasterxml.jackson.databind.JsonNode s : sentences) {
                    String t = s.path("text").asText("");
                    if (!t.isBlank()) sb.append(t);
                }
                if (sb.length() > 0) return sb.toString();
            }
            // 兜底：尝试顶层 text 字段
            String top = root.path("text").asText("");
            if (!top.isBlank()) return top;
        } catch (Exception ignored) {
            // 解析失败则退回原始串
        }
        return trimmed;
    }

    /** 用 ffmpeg 把任意音频转码成 16kHz 单声道 wav，返回临时文件。 */
    protected File transcode16kWav(String audioPath) throws Exception {
        File out = File.createTempFile("asr-", ".wav");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", audioPath,
                "-ac", "1", "-ar", "16000", "-f", "wav",
                out.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // 读掉输出，避免缓冲区填满阻塞
        String log;
        try (var in = p.getInputStream()) {
            log = new String(in.readAllBytes());
        }
        int code = p.waitFor();
        if (code != 0 || !out.exists() || out.length() == 0) {
            out.delete();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "音频转码失败（需安装 ffmpeg）: " + log.lines().reduce((a, b) -> b).orElse(""));
        }
        return out;
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
}
