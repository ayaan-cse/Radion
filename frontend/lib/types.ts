export type EventCategory = 'INTERVIEW' | 'TASK' | 'DEADLINE' | 'MEETING';
export type Platform = 'GMAIL' | 'WHATSAPP' | 'CLASSROOM' | 'OUTLOOK' | 'SLACK';

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
  accountEmail?: string;
  accountName?: string;
  accountAvatarUrl?: string;
}

// --- THIS IS THE NEW PART FOR PHASE 7 ---
export interface AnalyticsDTO {
  totalEventsAutomated: number;
  tasksPending: number;
  averageAiConfidence: number;
  hoursSaved: number;
}

export interface ClassroomAssignmentDTO {
  id: string;
  courseName: string;
  title: string;
  dueDate: string;
  status: string;
}

export interface ClassroomAnnouncementDTO {
  id: string;
  courseName: string;
  text: string;
  postedAt: string;
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
  analytics: AnalyticsDTO;
  upcomingAssignments: ClassroomAssignmentDTO[];
  overdueAssignments: ClassroomAssignmentDTO[];
  recentAnnouncements: ClassroomAnnouncementDTO[];
}
// ----------------------------------------

export interface NotificationDTO {
  id: string;
  title: string;
  content: string;
  timestamp: string;
  isRead: boolean;
}

export interface BusinessCommandDTO {
  commandType: 'REGISTER_OPPORTUNITY' | 'ADVANCE_OPPORTUNITY_STAGE' | 'SCHEDULE_INTERVIEW' | 'SCHEDULE_ASSESSMENT' | 'ASSIGN_ACTION_ITEM' | 'COMPLETE_ACTION_ITEM' | 'ANNOUNCE_EVENT';
  companyName?: string;
  title?: string;
  role?: string;
  stage?: string;
  ctc?: string;
  scheduledTime?: string;
  dueDate?: string;
  meetingLinkOrUrl?: string;
  description?: string;
  evidenceQuote?: string;
  executionPlan?: string;
  executionResult?: string;
  executionError?: string;
  calendarSyncResult?: string;
  calendarEventId?: string;
}

export interface DryRunSummaryDTO {
  totalEmails: number;
  impactfulCount: number;
  ignoredCount: number;
  uncertainCount: number;
  errorCount: number;
  totalCommandsPredicted: number;
}

export interface DryRunResultItemDTO {
  messageId: string;
  sender: string;
  subject: string;
  receivedAt: string;
  hasJourneyImpact: boolean;
  uncertainty: boolean;
  error: boolean;
  emailSummary: string;
  uncertaintyReason?: string;
  errorMessage?: string;
  processingDurationMs?: number;
  commands: BusinessCommandDTO[];
}

export interface DryRunResponseDTO {
  summary: DryRunSummaryDTO;
  results: DryRunResultItemDTO[];
}