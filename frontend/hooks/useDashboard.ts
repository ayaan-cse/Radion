import useSWR from 'swr';
import { apiClient } from '@/lib/api';

export function useDashboard() {
  const { data, error, isLoading, mutate } = useSWR('dashboardSummary', apiClient.getDashboardSummary, {
    refreshInterval: 60000, // Auto-refresh every minute
    revalidateOnFocus: true,
  });

  const triggerSync = async () => {
    try {
      await apiClient.syncNow();
      await mutate(); // Re-fetch data after sync
      return true;
    } catch (err) {
      console.error(err);
      return false;
    }
  };

  return {
    data,
    isLoading,
    isError: error,
    triggerSync,
  };
}