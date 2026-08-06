import { useState, useEffect } from "react";
import { RefreshCw, Bell } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { StatusDot } from "@/components/ui/StatusDot";
import { NotificationPanel } from "./NotificationPanel";
import { apiClient } from "@/lib/api";
import { NotificationDTO } from "@/lib/types";
import { useSession } from "next-auth/react";
import Link from "next/link";

interface TopBarProps {
  lastSyncTime: string;
  unreadCount: number;
  onSync: () => void;
  isSyncing: boolean;
}

export function TopBar({ lastSyncTime, unreadCount, onSync, isSyncing }: TopBarProps) {
  const { data: session } = useSession();
  const userId = session?.user?.id;
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);

  useEffect(() => {
    if (isNotifOpen && userId) {
      apiClient.getNotifications(userId).then(setNotifications).catch(console.error);
    }
  }, [isNotifOpen, userId]);

  const handleMarkAsRead = async (id: string) => {
    if (!userId) return;
    await apiClient.markNotificationAsRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
    );
  };

  return (
    <div className="flex items-center gap-2 md:gap-6 shrink-0">
      {/* Last sync — hidden on mobile to save space */}
      <span className="hidden md:block text-sm text-white/65 font-medium whitespace-nowrap">
        Last sync {lastSyncTime}
      </span>

      {/* Sync Button — icon-only on mobile */}
      <button
        onClick={onSync}
        disabled={isSyncing}
        className="flex items-center gap-1.5 px-2.5 py-1.5 md:px-4 md:py-2 rounded-full bg-glass backdrop-blur-md border border-glass-border text-white text-sm font-medium hover:bg-glass-hover transition-colors focus-visible:ring-2 focus-visible:ring-white/30 outline-none disabled:opacity-50"
      >
        <span className="hidden md:block text-sm">Sync Now</span>
        <motion.div
          animate={{ rotate: isSyncing ? 360 : 0 }}
          transition={{ repeat: isSyncing ? Infinity : 0, duration: 1, ease: "linear" }}
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </motion.div>
      </button>

      {/* Notification Bell */}
      <div className="relative">
        <button
          onClick={() => setIsNotifOpen(!isNotifOpen)}
          className="relative flex items-center justify-center w-8 h-8 md:w-10 md:h-10 rounded-full bg-glass backdrop-blur-md border border-glass-border hover:bg-glass-hover transition-colors focus-visible:ring-2 focus-visible:ring-white/30 outline-none"
        >
          <Bell className="w-4 h-4 text-white" />
          {unreadCount > 0 && (
            <span className="absolute top-0 right-0 w-2 h-2 bg-semantic-amber rounded-full border border-black/20" />
          )}
        </button>
        <AnimatePresence>
          {isNotifOpen && (
            <NotificationPanel
              notifications={notifications}
              onMarkAsRead={handleMarkAsRead}
            />
          )}
        </AnimatePresence>
      </div>

      {/* Avatar */}
      <Link
        href="/profile"
        className="relative w-8 h-8 md:w-10 md:h-10 rounded-full overflow-hidden border border-glass-border focus-visible:ring-2 focus-visible:ring-white/30 outline-none block shrink-0"
      >
        {session?.user?.image ? (
          <img
            src={session.user.image}
            alt={session.user.name || "Profile"}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full bg-white/20 backdrop-blur-md flex items-center justify-center text-white text-xs font-bold">
            {session?.user?.name?.[0] || "U"}
          </div>
        )}
        <StatusDot colorClass="bg-semantic-green absolute bottom-0 right-0 border-2 border-[#1a1a1a]" />
      </Link>
    </div>
  );
}