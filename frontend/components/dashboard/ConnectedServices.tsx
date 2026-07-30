import { ConnectionDTO, Platform } from "@/lib/types";
import { Mail, Calendar, MessageCircle, BookOpen, ArrowRight, Layers } from "lucide-react";
import Link from "next/link";

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
  if (isLoading) {
    return <div className="h-[72px] w-full mt-4 mb-6 rounded-2xl bg-white/5 animate-pulse border border-white/5" />;
  }

  const connectedList = connections.filter((c) => c.status === 'CONNECTED');

  return (
    <div className="w-full mt-4 mb-6">
      <div className="flex items-center justify-between gap-6 px-6 py-4 rounded-2xl bg-glass backdrop-blur-glass border border-glass-border shadow-glass-sm flex-wrap">
        {connectedList.length === 0 ? (
          <div className="flex items-center gap-4 flex-1 min-w-[280px]">
            <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/10 shrink-0">
              <Layers className="w-5 h-5 text-zinc-400" />
            </div>
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white leading-tight">No connected services</span>
              <span className="text-xs text-zinc-400 mt-0.5 leading-relaxed">
                Connect Gmail, Google Calendar or other integrations to let AI start monitoring your accounts.
              </span>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-4 flex-wrap flex-1 min-w-[280px]">
            <span className="text-xs font-bold text-zinc-400 uppercase tracking-wider mr-1 shrink-0">
              Connected Services:
            </span>
            <div className="flex items-center gap-3 flex-wrap">
              {connectedList.map((conn) => {
                const config = platformConfig[conn.platform] || { icon: Mail, name: conn.platform, color: "text-white" };
                const Icon = config.icon;
                return (
                  <div
                    key={conn.platform}
                    className="flex items-center gap-3 bg-white/5 px-3.5 py-2 rounded-xl border border-white/5 shadow-inner"
                  >
                    <div className="w-7 h-7 rounded-lg bg-white/5 flex items-center justify-center border border-white/5 shrink-0">
                      <Icon className={`w-3.5 h-3.5 ${config.color}`} />
                    </div>
                    <div className="flex flex-col">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-white leading-none">{config.name}</span>
                        {conn.accountEmail ? (
                          <span className="text-[10px] text-zinc-300 font-normal">({conn.accountEmail})</span>
                        ) : null}
                        <span className="text-[9px] font-extrabold px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 uppercase tracking-wider leading-none">
                          Connected
                        </span>
                      </div>
                      <span className="text-[10px] text-zinc-400 font-medium leading-tight mt-1">
                        Last sync: {conn.lastSyncAt || lastSyncTime || "Recently"}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <Link
          href="/integrations"
          className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold transition-all border border-white/10 shadow-sm hover:scale-[1.02] active:scale-[0.98] shrink-0"
        >
          <span>Manage Integrations</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>
    </div>
  );
}