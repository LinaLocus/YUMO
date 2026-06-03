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
