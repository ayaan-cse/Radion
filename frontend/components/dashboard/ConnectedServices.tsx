import { ConnectionDTO, Platform } from "@/lib/types";
import { Mail, Calendar, MessageCircle, BookOpen } from "lucide-react";

const platformConfig: Record<string, { icon: any; name: string; color: string }> = {
  GMAIL: { icon: Mail, name: "Gmail", color: "text-red-400" },
  GOOGLE_CALENDAR: { icon: Calendar, name: "Google Calendar", color: "text-blue-400" },
  WHATSAPP: { icon: MessageCircle, name: "WhatsApp", color: "text-green-400" },
  CLASSROOM: { icon: BookOpen, name: "Classroom", color: "text-yellow-400" },
  OUTLOOK: { icon: Mail, name: "Outlook", color: "text-sky-400" },
  SLACK: { icon: MessageCircle, name: "Slack", color: "text-purple-400" },
};

interface ConnectedServicesProps {
  connections: ConnectionDTO[];
  isLoading: boolean;
  lastSyncTime?: string;
}

export function ConnectedServices({ connections, isLoading, lastSyncTime }: ConnectedServicesProps) {
  if (isLoading) return null;

  const connectedList = connections.filter((c) => c.status === 'CONNECTED');

  if (connectedList.length === 0) {
    return null;
  }

  return (
    <div className="flex justify-end w-full mt-2 mb-6">
      <div className="flex items-center gap-5 px-5 py-2.5 rounded-2xl bg-glass backdrop-blur-glass border border-glass-border shadow-glass-sm">
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-zinc-300 uppercase tracking-wider">Connected Services:</span>
        </div>
        
        <div className="flex items-center gap-4">
          {connectedList.map((conn) => {
            const config = platformConfig[conn.platform] || { icon: Mail, name: conn.platform, color: "text-white" };
            const Icon = config.icon;
            return (
              <div key={conn.platform} className="flex items-center gap-2 bg-white/5 px-3 py-1 rounded-xl border border-white/5">
                <div className="w-4 h-4 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400 font-bold text-[10px]">
                  ✓
                </div>
                <Icon className={`w-3.5 h-3.5 ${config.color}`} />
                <span className="text-xs font-semibold text-white">{config.name}</span>
              </div>
            );
          })}
        </div>

        <div className="h-4 w-px bg-white/10" />

        <div className="flex items-center gap-1.5 text-[11px] text-zinc-400 font-medium">
          <span>Last sync:</span>
          <span className="text-zinc-300">{lastSyncTime || connectedList[0]?.lastSyncAt || "Recently"}</span>
        </div>
      </div>
    </div>
  );
}