import { DashboardSummaryDTO, NotificationDTO } from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

export const apiClient = {
  async getDashboardSummary(userId: string, searchQuery?: string): Promise<DashboardSummaryDTO> {
    const url = new URL(`${API_BASE_URL}/dashboard/summary`);
    url.searchParams.append("userId", userId);
    if (searchQuery) {
      url.searchParams.append("search", searchQuery);
    }
    
    const res = await fetch(url.toString());
    if (!res.ok) throw new Error("Failed to fetch dashboard data");
    return res.json();
  },

  async syncNow(userId: string): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/integrations/sync?userId=${userId}`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error("Sync failed");
  },

  async getNotifications(userId: string): Promise<NotificationDTO[]> {
    const res = await fetch(`${API_BASE_URL}/notifications?userId=${userId}`);
    if (!res.ok) throw new Error("Failed to fetch notifications");
    return res.json();
  },

  async markNotificationAsRead(id: string): Promise<void> {
    await fetch(`${API_BASE_URL}/notifications/${id}/read`, { method: 'POST' });
  }
};