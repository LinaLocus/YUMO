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
