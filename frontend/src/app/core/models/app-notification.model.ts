export type AppNotificationType = 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: AppNotificationType;
  link: string | null;
  read: boolean;
  createdAt: string;
}
