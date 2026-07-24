import { DashboardSummaryDTO, NotificationDTO } from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";
const USER_ID = "00000000-0000-0000-0000-000000000000"; // Hardcoded for demo

export const apiClient = {
  async getDashboardSummary(searchQuery?: string): Promise<DashboardSummaryDTO> {
    const url = new URL(`${API_BASE_URL}/dashboard/summary`);
    url.searchParams.append("userId", USER_ID);
    if (searchQuery) {
      url.searchParams.append("search", searchQuery);
    }
    
    const res = await fetch(url.toString());
    if (!res.ok) throw new Error("Failed to fetch dashboard data");
    return res.json();
  },

  async syncNow(): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/integrations/sync?userId=${USER_ID}`, {
      method: 'POST',
    });
    if (!res.ok) throw new Error("Sync failed");
  },

  async getNotifications(): Promise<NotificationDTO[]> {
    const res = await fetch(`${API_BASE_URL}/notifications?userId=${USER_ID}`);
    if (!res.ok) throw new Error("Failed to fetch notifications");
    return res.json();
  },

  async markNotificationAsRead(id: string): Promise<void> {
    await fetch(`${API_BASE_URL}/notifications/${id}/read`, { method: 'POST' });
  }
};