"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { Mail, BookOpen, MessageCircle, Calendar, RefreshCw, Unplug, CheckCircle2 } from "lucide-react";
import { useDashboard } from "@/hooks/useDashboard";
import { useSession } from "next-auth/react";
import { useToast } from "@/hooks/useToast";
import { Platform } from "@/lib/types";
import { useState, useEffect } from "react";

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
  onConnect: () => void;
  onSync: () => Promise<void>;
  onDisconnect: () => void;
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
  onConnect,
  onSync,
  onDisconnect,
}: IntegrationCardProps) {
  const [syncing, setSyncing] = useState(false);

  const handleSyncClick = async () => {
    setSyncing(true);
    try {
      await onSync();
    } finally {
      setSyncing(false);
    }
  };

  return (
    <GlassCard className="p-6 flex flex-col items-start relative overflow-hidden group">
      {isConnected && (
        <div className="absolute top-4 right-4 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-[11px] font-bold tracking-wider uppercase">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>Connected</span>
        </div>
      )}

      <div className={`w-12 h-12 rounded-xl ${bgColor} flex items-center justify-center mb-4 border ${borderColor} shadow-sm group-hover:scale-105 transition-transform`}>
        <Icon className={`w-6 h-6 ${iconColor}`} />
      </div>

      <h3 className="text-lg font-bold text-white mb-1">{title}</h3>
      <p className="text-sm text-white/60 mb-6 flex-1 leading-relaxed">{description}</p>

      {isConnected ? (
        <div className="w-full flex flex-col gap-3 pt-2 border-t border-white/5">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span>Last synced:</span>
            <span className="text-zinc-300 font-medium">{lastSyncAt || "Recently"}</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={handleSyncClick}
              disabled={syncing}
              className="flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-lg bg-blue-500/20 hover:bg-blue-500/30 text-blue-300 text-xs font-semibold transition-colors border border-blue-500/30 disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${syncing ? "animate-spin" : ""}`} />
              <span>{syncing ? "Syncing..." : "Sync Now"}</span>
            </button>
            <button
              onClick={onDisconnect}
              className="flex items-center justify-center py-2 px-3 rounded-lg bg-white/5 hover:bg-red-500/20 text-zinc-400 hover:text-red-400 text-xs font-semibold transition-colors border border-white/10 hover:border-red-500/30"
              title="Disconnect Integration"
            >
              <Unplug className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      ) : isComingSoon ? (
        <button className="w-full py-2 rounded-lg bg-white/5 text-white/40 text-sm font-medium border border-white/5 cursor-not-allowed">
          Coming Soon
        </button>
      ) : (
        <button
          onClick={onConnect}
          className="w-full py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition-colors border border-white/10 shadow-sm"
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

  const handleDisconnect = () => {
    addToast("To disconnect an integration, please revoke access in your Google Account security settings.", "info");
  };

  const getConnection = (platform: Platform) => {
    return data?.connections?.find((c) => c.platform === platform && c.status === "CONNECTED");
  };

  return (
    <div className="flex-1 flex flex-col pt-4">
      <h1 className="text-2xl font-bold text-white tracking-tight mb-2">Integrations</h1>
      <p className="text-sm text-white/60 mb-8">
        Connect and manage your external accounts to allow AI to monitor, sync, and extract tasks automatically.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <IntegrationCard
          id="GMAIL"
          title="Gmail"
          description="Extract interviews, project deadlines, and urgent placement updates directly from your inbox."
          icon={Mail}
          iconColor="text-red-400"
          bgColor="bg-red-500/20"
          borderColor="border-red-500/30"
          isConnected={!!getConnection("GMAIL")}
          lastSyncAt={getConnection("GMAIL")?.lastSyncAt}
          onConnect={() => handleConnect("GMAIL")}
          onSync={handleSync}
          onDisconnect={handleDisconnect}
        />

        <IntegrationCard
          id="GOOGLE_CALENDAR"
          title="Google Calendar"
          description="Two-way sync for your daily schedule, academic lectures, and interview slots."
          icon={Calendar}
          iconColor="text-blue-400"
          bgColor="bg-blue-500/20"
          borderColor="border-blue-500/30"
          isConnected={!!getConnection("GOOGLE_CALENDAR")}
          lastSyncAt={getConnection("GOOGLE_CALENDAR")?.lastSyncAt}
          onConnect={() => handleConnect("GOOGLE_CALENDAR")}
          onSync={handleSync}
          onDisconnect={handleDisconnect}
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
          onConnect={() => handleConnect("CLASSROOM")}
          onSync={handleSync}
          onDisconnect={handleDisconnect}
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
          onSync={async () => {}}
          onDisconnect={() => {}}
        />
      </div>
    </div>
  );
}