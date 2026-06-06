export type TranscriptionStatus =
  | 'UPLOADED' | 'TRANSCRIBING' | 'TRANSCRIBED'
  | 'SUMMARIZING' | 'DONE' | 'FAILED';

export type SummaryTemplate = 'MEETING' | 'LECTURE' | 'GENERAL';

export type SummaryLanguage = 'AUTO' | 'CHINESE' | 'ENGLISH';

export interface Detail {
  id: number;
  originalFilename: string;
  status: TranscriptionStatus;
  transcriptText: string | null;
  summaryMarkdown: string | null;
  hasTtsAudio: boolean;
  template: SummaryTemplate;
  errorMessage: string | null;
  createdAt: string;
}

export interface ListItem {
  id: number;
  originalFilename: string;
  status: TranscriptionStatus;
  template: SummaryTemplate;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  username: string;
}
