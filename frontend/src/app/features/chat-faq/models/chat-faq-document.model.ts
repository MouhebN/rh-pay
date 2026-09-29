export type FaqCategory = 'LEAVE' | 'PAYROLL' | 'ATTENDANCE' | 'GENERAL';

export interface ChatFaqDocument {
  id: number;
  title: string;
  category: FaqCategory;
  fileName: string;
  contentType: string | null;
  uploadedByUsername: string | null;
  createdAt: string;
}
