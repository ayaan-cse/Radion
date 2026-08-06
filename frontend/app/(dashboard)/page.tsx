"use client";

import { useDashboard } from "@/hooks/useDashboard";
import { useToast } from "@/hooks/useToast";
import { TopBar } from "@/components/dashboard/TopBar";
import { ConnectedServices } from "@/components/dashboard/ConnectedServices";
import { GlassCard } from "@/components/ui/GlassCard";
import { TodaysEvents } from "@/components/dashboard/TodaysEvents";
import { UpcomingEvents } from "@/components/dashboard/UpcomingEvents";
import { RecentMessages } from "@/components/dashboard/RecentMessages";
import { EmptyState } from "@/components/shared/EmptyState";
import { ClassroomWidget } from "@/components/dashboard/ClassroomWidget";
import { motion } from "framer-motion";

export default function DashboardPage() {
  const { data, isLoading, isError, triggerSync } = useDashboard();
  const { addToast } = useToast();

  const handleManualSync = async () => {
    const success = await triggerSync();

    if (success) {
      addToast("Sync completed successfully", "success");
    } else {
      addToast("Sync failed. Please try again.", "error");
    }
  };

  if (isError) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <GlassCard className="p-8 text-center">
          <h2 className="text-white text-xl font-bold mb-2">
            Connection Error
          </h2>

          <p className="text-white/60">
            Failed to load dashboard data. Please ensure the backend is running.
          </p>
        </GlassCard>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <p className="text-white/60">Loading session...</p>
      </div>
    );
  }

  return (
    <>
      <header className="flex flex-col gap-2 w-full md:flex-row md:items-center md:justify-between md:gap-0">
        <div className="flex flex-col">
          <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight">
            {isLoading
              ? "Loading..."
              : `Good Morning, ${data?.user.firstName} 👋`}
          </h1>

          <p className="text-xs md:text-sm text-white/60 mt-1">
            AI is monitoring your connected accounts and turning important
            messages into events.
          </p>
        </div>

        <TopBar
          lastSyncTime={data?.lastSyncTime || "Syncing..."}
          unreadCount={data?.unreadNotifications || 0}
          onSync={handleManualSync}
          isSyncing={isLoading}
        />
      </header>


      <ConnectedServices
        connections={data?.connections || []}
        isLoading={isLoading}
        lastSyncTime={data?.lastSyncTime}
      />

      <motion.div
        className="flex-1 flex flex-col gap-6 pb-2"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, staggerChildren: 0.1 }}
      >
        {/* Today's Events */}
        <GlassCard className="flex-1 flex flex-col p-6">
          <h2 className="text-white font-bold text-[17px] tracking-wide mb-4">
            Today's Events
          </h2>

          <div className="flex-1 relative overflow-y-auto pr-2 custom-scrollbar">
            {isLoading ? (
              <EmptyState message="Loading events..." />
            ) : data?.todaysEvents.length === 0 ? (
              <EmptyState message="No events scheduled for today." />
            ) : (
              <TodaysEvents events={data!.todaysEvents} />
            )}
          </div>
        </GlassCard>

        {/* Upcoming Events */}
        <GlassCard className="flex-1 flex flex-col p-6">
          <h2 className="text-white font-bold text-[17px] tracking-wide mb-4">
            Upcoming Events
          </h2>

          <div className="flex-1 relative overflow-y-auto pr-2 custom-scrollbar">
            {isLoading ? (
              <EmptyState message="Loading timeline..." />
            ) : data?.upcomingEvents.length === 0 ? (
              <EmptyState message="No upcoming events found." />
            ) : (
              <UpcomingEvents events={data!.upcomingEvents} />
            )}
          </div>
        </GlassCard>

        {/* Classroom Widget */}
        <ClassroomWidget 
          upcomingAssignments={data?.upcomingAssignments} 
          overdueAssignments={data?.overdueAssignments} 
          recentAnnouncements={data?.recentAnnouncements} 
        />

        {/* Recent Messages */}
        <GlassCard className="flex-1 flex flex-col p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-white font-bold text-[17px] tracking-wide">
              Recent Ai Processed Messages
            </h2>

            <button className="text-sm text-white/60 hover:text-white transition-colors">
              View all
            </button>
          </div>

          <div className="flex-1 relative overflow-y-auto pr-2 custom-scrollbar">
            {isLoading ? (
              <EmptyState message="Loading messages..." />
            ) : data?.recentMessages.length === 0 ? (
              <EmptyState message="No recent messages processed." />
            ) : (
              <RecentMessages messages={data!.recentMessages} />
            )}
          </div>
        </GlassCard>
      </motion.div>
    </>
  );
}