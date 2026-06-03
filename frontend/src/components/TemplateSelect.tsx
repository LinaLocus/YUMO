import type { SummaryTemplate } from '@/types';

const LABELS: Record<SummaryTemplate, string> = {
  MEETING: '会议纪要',
  LECTURE: '课堂笔记',
  GENERAL: '通用概括',
};

export function TemplateSelect({
  value, onChange, disabled,
}: { value: SummaryTemplate; onChange: (v: SummaryTemplate) => void; disabled?: boolean }) {
  return (
    <select
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value as SummaryTemplate)}
      aria-label="概括模板"
      className="rounded-lg border border-[#99F6E4] bg-white/90 dark:bg-white/5 px-3 py-1.5 text-sm cursor-pointer"
    >
      {(Object.keys(LABELS) as SummaryTemplate[]).map((k) => (
        <option key={k} value={k}>{LABELS[k]}</option>
      ))}
    </select>
  );
}
