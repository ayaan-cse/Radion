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
      <div className="flex-1 flex items-center justify-center p-4">
        <GlassCard className="p-6 text-center max-w-sm w-full">
          <h2 className="text-white text-base font-bold mb-2">Connection Error</h2>
          <p className="text-white/60 text-sm">
            Failed to load dashboard data. Please ensure the backend is running.
          </p>
        </GlassCard>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <p className="text-white/60 text-sm">Loading session...</p>
      </div>
    );
  }

  return (
    <>
      {/* Compact header — stacks on mobile, row on desktop */}
      <header className="flex items-start justify-between w-full gap-2 mb-0">
        <div className="flex flex-col min-w-0 flex-1">
          <h1 className="text-[20px] md:text-2xl font-bold text-white tracking-tight leading-snug">
            {isLoading ? "Loading…" : `Good Morning, ${data?.user.firstName} 👋`}
          </h1>
          <p className="text-[11px] md:text-sm text-white/55 mt-0.5 line-clamp-1 md:line-clamp-none leading-snug">
            AI is monitoring your accounts and turning important messages into events.
          </p>
        </div>

        <TopBar
          lastSyncTime={data?.lastSyncTime || "Syncing..."}
          unreadCount={data?.unreadNotifications || 0}
          onSync={handleManualSync}
          isSyncing={isLoading}
        />
      </header>

      {/* Connected services — compact strip */}
      <ConnectedServices
        connections={data?.connections || []}
        isLoading={isLoading}
        lastSyncTime={data?.lastSyncTime}
      />

      {/* Main content cards — stack naturally on mobile, fill screen on desktop */}
      <motion.div
        className="flex flex-col gap-4 md:gap-5 pb-4 flex-1 min-h-0"
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
        {/* Today's Events */}
        <GlassCard className="flex flex-col p-4 md:p-6 h-[260px] md:h-auto md:flex-1">
          <h2 className="text-white font-bold text-[14px] md:text-[17px] tracking-wide mb-2 md:mb-4 shrink-0">
            Today&apos;s Events
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
        <GlassCard className="flex flex-col p-4 md:p-6 h-[260px] md:h-auto md:flex-1">
          <h2 className="text-white font-bold text-[14px] md:text-[17px] tracking-wide mb-2 md:mb-4 shrink-0">
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

        {/* Recent AI Messages */}
        <GlassCard className="flex flex-col p-4 md:p-6 h-[200px] md:h-auto md:flex-1">
          <div className="flex items-center justify-between mb-2 md:mb-4 shrink-0">
            <h2 className="text-white font-bold text-[14px] md:text-[17px] tracking-wide">
              Recent AI Messages
            </h2>
            <button className="text-[11px] text-white/60 hover:text-white transition-colors">
              View all
            </button>
          </div>
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