import useSWR from 'swr';
import { apiClient } from '@/lib/api';
import { useSession } from 'next-auth/react';

export function useDashboard() {
  const { data: session } = useSession();
  const userId = session?.user?.id;

  const { data, error, isLoading, mutate } = useSWR(
    userId ? ['dashboardSummary', userId] : null,
    ([, id]) => apiClient.getDashboardSummary(id as string),
    {
      refreshInterval: 60000, // Auto-refresh every minute
      revalidateOnFocus: true,
    }
  );

  const triggerSync = async () => {
    if (!userId) return false;
    try {
      await apiClient.syncNow(userId);
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