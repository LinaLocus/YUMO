/** 已验证可用的 MiniMax 系统音色（voice_id + 中文显示名）。 */
export interface VoiceOption {
  id: string;
  name: string;
}

export const VOICES: VoiceOption[] = [
  { id: 'male-qn-qingse', name: '青涩青年（男）' },
  { id: 'male-qn-jingying', name: '精英青年（男）' },
  { id: 'male-qn-badao', name: '霸道青年（男）' },
  { id: 'male-qn-daxuesheng', name: '青年大学生（男）' },
  { id: 'female-shaonv', name: '少女（女）' },
  { id: 'female-yujie', name: '御姐（女）' },
  { id: 'female-chengshu', name: '成熟女性（女）' },
  { id: 'female-tianmei', name: '甜美女性（女）' },
  { id: 'presenter_male', name: '男主持人' },
  { id: 'presenter_female', name: '女主持人' },
  { id: 'audiobook_male_1', name: '有声书男声' },
  { id: 'audiobook_female_1', name: '有声书女声' },
  { id: 'clever_boy', name: '聪明男童' },
  { id: 'cute_boy', name: '可爱男童' },
];

export const DEFAULT_VOICE = VOICES[0].id;
