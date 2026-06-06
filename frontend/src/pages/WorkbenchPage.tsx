import { useCallback, useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { streamSummary } from '@/lib/sse';
import { auth } from '@/store/auth';
import type { Detail, ListItem, SummaryTemplate, SummaryLanguage } from '@/types';
import { Dropzone } from '@/components/Dropzone';
import { TranscribeProgress } from '@/components/TranscribeProgress';
import { SummaryView } from '@/components/SummaryView';
import { AudioPlayer } from '@/components/AudioPlayer';
import { Toolbar } from '@/components/Toolbar';
import { TemplateSelect } from '@/components/TemplateSelect';
import { LanguageSelect } from '@/components/LanguageSelect';
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
  const [language, setLanguage] = useState<SummaryLanguage>('AUTO');
  const [view, setView] = useState<View>('idle');
  const [markdown, setMarkdown] = useState('');
  const [streaming, setStreaming] = useState(false);
  const [showAudio, setShowAudio] = useState(false);
  const [speaking, setSpeaking] = useState(false);
  const [speechVersion, setSpeechVersion] = useState(0);
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
      const up = await api.upload(file, template, language);
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
    setSpeechVersion((v) => v + 1);
    setShowAudio(true);
  }

  return (
    <div className="flex min-h-dvh flex-col">
      <header className="flex items-center justify-between border-b border-[#99F6E4]/40 px-4 py-3">
        <div className="flex items-baseline gap-3">
          <span className="text-lg font-bold">语墨 <span className="text-sm font-normal opacity-60">EchoInk</span></span>
          <span className="hidden sm:inline text-xs italic opacity-50">“声驻韶华化作墨，字留方寸赋新篇”</span>
        </div>
        <div className="flex items-center gap-2">
          <TemplateSelect value={template} onChange={setTemplate} disabled={view === 'transcribing' || streaming} />
          <LanguageSelect value={language} onChange={setLanguage} disabled={view === 'transcribing' || streaming} />
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
                  {showAudio && activeId && <AudioPlayer id={activeId} version={speechVersion} />}
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
