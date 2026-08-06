"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { Mail, BookOpen, MessageCircle, Calendar, RefreshCw, Unplug, CheckCircle2, UserCheck, ArrowRightLeft, ChevronDown } from "lucide-react";
import { useDashboard } from "@/hooks/useDashboard";
import { useSession } from "next-auth/react";
import { useToast } from "@/hooks/useToast";
import { Platform } from "@/lib/types";
import { useState, useEffect } from "react";
import { apiClient } from "@/lib/api";

interface IntegrationCardProps {
  id: Platform;
  title: string;
  description: string;
  icon: any;
  iconColor: string;
  bgColor: string;
  borderColor: string;
  isComingSoon?: boolean;
  isConnected: boolean;
  lastSyncAt?: string;
  accountEmail?: string;
  accountName?: string;
  accountAvatarUrl?: string;
  onConnect: () => void;
  onSwitchAccount: () => void;
  onSync: () => Promise<void>;
  onDisconnect: () => Promise<void>;
}

function IntegrationCard({
  title,
  description,
  icon: Icon,
  iconColor,
  bgColor,
  borderColor,
  isComingSoon,
  isConnected,
  lastSyncAt,
  accountEmail,
  accountName,
  accountAvatarUrl,
  onConnect,
  onSwitchAccount,
  onSync,
  onDisconnect,
}: IntegrationCardProps) {
  const [syncing, setSyncing] = useState(false);
  const [disconnecting, setDisconnecting] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const handleSyncClick = async () => {
    setSyncing(true);
    try {
      await onSync();
    } finally {
      setSyncing(false);
    }
  };

  const handleDisconnectClick = async () => {
    setDisconnecting(true);
    try {
      await onDisconnect();
    } finally {
      setDisconnecting(false);
    }
  };

  return (
    <GlassCard className="p-4 md:p-6 flex flex-col items-start relative overflow-hidden group transition-all duration-300">
      {isConnected && (
        <div className="absolute top-4 right-4 flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[10px] md:text-[11px] font-bold tracking-wider uppercase z-10 pointer-events-none">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span className="hidden sm:block">Connected</span>
        </div>
      )}

      {/* Clickable Header Area for Expand/Collapse */}
      <div 
        className="w-full cursor-pointer flex flex-col relative z-0"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <div className="flex justify-between items-start w-full mb-3">
          <div className={`w-10 h-10 md:w-12 md:h-12 rounded-xl ${bgColor} flex items-center justify-center border ${borderColor} shadow-sm group-hover:scale-105 transition-transform`}>
            <Icon className={`w-5 h-5 md:w-6 md:h-6 ${iconColor}`} />
          </div>
          <button className="text-white/40 hover:text-white/80 transition-colors p-1 rounded-md mt-1 mr-12 sm:mr-0">
            <ChevronDown className={`w-5 h-5 transition-transform duration-300 ${isExpanded ? "rotate-180" : ""}`} />
          </button>
        </div>

        <h3 className="text-base md:text-lg font-bold text-white mb-1">{title}</h3>
        
        <div className={`grid transition-all duration-300 ease-in-out ${isExpanded ? "grid-rows-[1fr] opacity-100 mb-4" : "grid-rows-[0fr] opacity-0"}`}>
          <div className="overflow-hidden">
            <p className="text-[13px] md:text-sm text-white/60 leading-relaxed pb-2">{description}</p>
          </div>
        </div>
        {!isExpanded && <div className="h-2"></div>}
      </div>

      {isConnected ? (
        <div className="w-full flex flex-col gap-3 pt-3 border-t border-white/10 mt-auto">
          {/* Connected Account Metadata */}
          <div className="flex items-center gap-3 p-2.5 rounded-xl bg-white/5 border border-white/5">
            <div className="w-8 h-8 rounded-full overflow-hidden bg-white/10 flex items-center justify-center shrink-0 border border-white/10">
              {accountAvatarUrl ? (
                <img src={accountAvatarUrl} alt={accountName || "Account Avatar"} className="w-full h-full object-cover" />
              ) : (
                <UserCheck className="w-4 h-4 text-emerald-400" />
              )}
            </div>
            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-xs font-bold text-white truncate">{accountName || "Connected Account"}</span>
              <span className="text-[11px] text-zinc-400 truncate">{accountEmail || "Active Connection"}</span>
            </div>
          </div>

          <div className="flex items-center justify-between text-xs text-zinc-400 px-1">
            <span>Last synced:</span>
            <span className="text-zinc-300 font-medium">{lastSyncAt || "Recently"}</span>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2 pt-1">
            <button
              onClick={handleSyncClick}
              disabled={syncing || disconnecting}
              className="flex-1 flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 text-xs font-semibold transition-colors border border-blue-500/30 disabled:opacity-50"
              title="Sync Now"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${syncing ? "animate-spin" : ""}`} />
              <span>{syncing ? "Syncing..." : "Sync Now"}</span>
            </button>
            <button
              onClick={onSwitchAccount}
              disabled={syncing || disconnecting}
              className="flex items-center justify-center gap-1.5 py-2 px-2.5 rounded-lg bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-colors border border-white/15 disabled:opacity-50"
              title="Switch Account (Starts OAuth immediately without disconnecting)"
            >
              <ArrowRightLeft className="w-3.5 h-3.5" />
              <span>Switch</span>
            </button>
            <button
              onClick={handleDisconnectClick}
              disabled={syncing || disconnecting}
              className="flex items-center justify-center py-2 px-2.5 rounded-lg bg-white/5 hover:bg-red-500/20 text-zinc-400 hover:text-red-400 text-xs font-semibold transition-colors border border-white/10 hover:border-red-500/30 disabled:opacity-50"
              title="Disconnect Service (Removes connection only, never logs you out of Radion)"
            >
              <Unplug className={`w-3.5 h-3.5 ${disconnecting ? "animate-pulse text-red-400" : ""}`} />
            </button>
          </div>
        </div>
      ) : isComingSoon ? (
        <button className="w-full py-2.5 rounded-xl bg-white/5 text-white/40 text-sm font-medium border border-white/5 cursor-not-allowed mt-auto">
          Coming Soon
        </button>
      ) : (
        <button
          onClick={onConnect}
          className="w-full py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-sm font-semibold transition-all border border-white/15 shadow-sm hover:scale-[1.02] active:scale-[0.98] mt-auto"
        >
          Connect {title}
        </button>
      )}
    </GlassCard>
  );
}

export default function IntegrationsPage() {
  const { data, triggerSync } = useDashboard();
  const { data: session } = useSession();
  const { addToast } = useToast();
  const userId = session?.user?.id;

  useEffect(() => {
    if (typeof window === "undefined") return;
    const params = new URLSearchParams(window.location.search);
    const connection = params.get("connection");
    const platform = params.get("platform");
    const reason = params.get("reason");

    if (connection === "success") {
      addToast(`Successfully connected ${platform || "service"}!`, "success");
      triggerSync();
      window.history.replaceState({}, "", "/integrations");
    } else if (connection === "error") {
      addToast(`Failed to connect integration: ${reason || "error"}`, "error");
      window.history.replaceState({}, "", "/integrations");
    }
  }, [addToast, triggerSync]);

  const handleConnect = (platform: Platform) => {
    if (!userId) {
      addToast("You must be logged in to connect integrations.", "error");
      return;
    }
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";
    window.location.href = `${apiUrl}/integrations/google/connect?platform=${platform}&userId=${userId}`;
  };

  const handleSync = async () => {
    const success = await triggerSync();
    if (success) {
      addToast("Manual sync initiated successfully.", "success");
    } else {
      addToast("Sync failed. Please check backend logs.", "error");
    }
  };

  const handleDisconnect = async (platform: Platform) => {
    if (!userId) return;
    try {
      await apiClient.disconnectIntegration(platform, userId);
      addToast(`Disconnected ${platform} successfully.`, "info");
      await triggerSync();
    } catch (err) {
      addToast(`Failed to disconnect ${platform}.`, "error");
    }
  };

  const getConnection = (platform: Platform) => {
    return data?.connections?.find((c) => c.platform === platform && c.status === "CONNECTED");
  };

  return (
    <div className="flex-1 flex flex-col pt-2 md:pt-4 overflow-y-auto custom-scrollbar px-0">
      <div className="flex flex-col md:flex-row md:items-end justify-between mb-4 md:mb-8 gap-2">
        <div>
          <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight mb-1">Services & Integrations</h1>
          <p className="text-[11px] md:text-sm text-white/60 max-w-2xl">
            Each service operates independently with its own Google account identity.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6 pb-12">
        <IntegrationCard
          id="GMAIL"
          title="Gmail"
          description="Connect your Google account once. Radion will monitor Gmail, understand placement emails using AI, and automatically create interview schedules, deadlines, reminders, and tasks inside your Google Calendar."
          icon={Mail}
          iconColor="text-red-400"
          bgColor="bg-red-500/20"
          borderColor="border-red-500/30"
          isConnected={!!getConnection("GMAIL")}
          lastSyncAt={getConnection("GMAIL")?.lastSyncAt}
          accountEmail={getConnection("GMAIL")?.accountEmail}
          accountName={getConnection("GMAIL")?.accountName}
          accountAvatarUrl={getConnection("GMAIL")?.accountAvatarUrl}
          onConnect={() => handleConnect("GMAIL")}
          onSwitchAccount={() => handleConnect("GMAIL")}
          onSync={handleSync}
          onDisconnect={() => handleDisconnect("GMAIL")}
        />



        <IntegrationCard
          id="CLASSROOM"
          title="Google Classroom"
          description="Automatically import coursework, assignment due dates, and classroom announcements."
          icon={BookOpen}
          iconColor="text-yellow-400"
          bgColor="bg-yellow-500/20"
          borderColor="border-yellow-500/30"
          isConnected={!!getConnection("CLASSROOM")}
          lastSyncAt={getConnection("CLASSROOM")?.lastSyncAt}
          accountEmail={getConnection("CLASSROOM")?.accountEmail}
          accountName={getConnection("CLASSROOM")?.accountName}
          accountAvatarUrl={getConnection("CLASSROOM")?.accountAvatarUrl}
          onConnect={() => handleConnect("CLASSROOM")}
          onSwitchAccount={() => handleConnect("CLASSROOM")}
          onSync={handleSync}
          onDisconnect={() => handleDisconnect("CLASSROOM")}
        />

        <IntegrationCard
          id="WHATSAPP"
          title="WhatsApp"
          description="Monitor placement cell and academic WhatsApp groups for real-time notifications."
          icon={MessageCircle}
          iconColor="text-green-400"
          bgColor="bg-green-500/20"
          borderColor="border-green-500/30"
          isComingSoon
          isConnected={false}
          onConnect={() => {}}
          onSwitchAccount={() => {}}
          onSync={async () => {}}
          onDisconnect={async () => {}}
        />
      </div>
    </div>
  );
}