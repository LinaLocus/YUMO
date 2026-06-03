import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Download, Copy, Volume2 } from 'lucide-react';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';
import { toast } from 'sonner';
import { VOICES, DEFAULT_VOICE } from '@/lib/voices';

interface Props {
  id: number;
  markdown: string;
  onSpeechReady: () => void;
  speaking: boolean;
}

export function Toolbar({ id, markdown, onSpeechReady, speaking }: Props) {
  const [busy, setBusy] = useState(false);
  const [voice, setVoice] = useState(DEFAULT_VOICE);
  const generating = speaking || busy;

  async function copy() {
    await navigator.clipboard.writeText(markdown);
    toast.success('已复制到剪贴板');
  }

  function downloadMd() {
    const token = auth.getToken() ?? '';
    const a = document.createElement('a');
    a.href = `${api.downloadUrl(id)}?token=${encodeURIComponent(token)}`;
    a.download = `summary-${id}.md`;
    a.click();
  }

  async function speak() {
    setBusy(true);
    try {
      await api.generateSpeech(id, voice);
      toast.success('朗读音频已生成');
      onSpeechReady();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '朗读生成失败');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Button variant="accent" onClick={downloadMd}>
        <Download className="h-4 w-4" /> 下载 .md
      </Button>
      <Button variant="ghost" onClick={copy}>
        <Copy className="h-4 w-4" /> 复制
      </Button>
      <select
        aria-label="朗读音色"
        value={voice}
        disabled={generating}
        onChange={(e) => setVoice(e.target.value)}
        className="rounded-lg border border-[#99F6E4] bg-white/90 dark:bg-white/5 px-2 py-1.5 text-sm cursor-pointer"
      >
        {VOICES.map((v) => (
          <option key={v.id} value={v.id}>{v.name}</option>
        ))}
      </select>
      <Button variant="ghost" onClick={speak} disabled={generating}>
        <Volume2 className="h-4 w-4" /> {generating ? '生成中…' : '朗读'}
      </Button>
    </div>
  );
}
