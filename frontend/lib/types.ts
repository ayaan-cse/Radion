export type EventCategory = 'INTERVIEW' | 'TASK' | 'DEADLINE' | 'MEETING';
export type Platform = 'GMAIL' | 'GOOGLE_CALENDAR' | 'WHATSAPP' | 'CLASSROOM' | 'OUTLOOK' | 'SLACK';

export interface EventDTO {
  id: string;
  time: string;
  title: string;
  source: string;
  category: EventCategory;
}

export interface UpcomingEventDTO {
  id: string;
  day: string;
  month: string;
  company: string;
  title: string;
  time: string;
  category: EventCategory;
}

export interface MessageDTO {
  id: string;
  platform: Platform;
  title: string;
  summary: string;
  timestamp: string;
  isUnread: boolean;
}

export interface ConnectionDTO {
  platform: Platform;
  status: 'CONNECTED' | 'DISCONNECTED' | 'ERROR';
  lastSyncAt?: string;
}

// --- THIS IS THE NEW PART FOR PHASE 7 ---
export interface AnalyticsDTO {
  totalEventsAutomated: number;
  tasksPending: number;
  averageAiConfidence: number;
  hoursSaved: number;
}

export interface DashboardSummaryDTO {
  user: {
    firstName: string;
    avatarUrl: string;
  };
  lastSyncTime: string;
  unreadNotifications: number;
  connections: ConnectionDTO[];
  todaysEvents: EventDTO[];
  upcomingEvents: UpcomingEventDTO[];
  recentMessages: MessageDTO[];
  analytics: AnalyticsDTO; // Added here
}
// ----------------------------------------

export interface NotificationDTO {
  id: string;
  title: string;
  content: string;
  timestamp: string;
  isRead: boolean;
}