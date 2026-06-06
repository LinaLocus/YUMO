import type { SummaryLanguage } from '@/types';

const LABELS: Record<SummaryLanguage, string> = {
  AUTO: '跟随原文',
  CHINESE: '中文',
  ENGLISH: '英文',
};

export function LanguageSelect({
  value, onChange, disabled,
}: { value: SummaryLanguage; onChange: (v: SummaryLanguage) => void; disabled?: boolean }) {
  return (
    <select
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value as SummaryLanguage)}
      aria-label="概括语言"
      className="rounded-lg border border-[#99F6E4] bg-white/90 dark:bg-white/5 px-3 py-1.5 text-sm cursor-pointer"
    >
      {(Object.keys(LABELS) as SummaryLanguage[]).map((k) => (
        <option key={k} value={k}>{LABELS[k]}</option>
      ))}
    </select>
  );
}
