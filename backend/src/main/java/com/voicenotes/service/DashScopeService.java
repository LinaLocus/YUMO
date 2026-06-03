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
