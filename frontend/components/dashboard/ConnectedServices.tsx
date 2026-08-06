import { ConnectionDTO } from "@/lib/types";
import { Mail, Calendar, MessageCircle, BookOpen, ArrowRight, Layers } from "lucide-react";
import Link from "next/link";

const platformConfig: Record<string, { icon: any; name: string; color: string }> = {
  GMAIL: { icon: Mail, name: "Gmail", color: "text-red-400" },
  GOOGLE_CALENDAR: { icon: Calendar, name: "Calendar", color: "text-blue-400" },
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

export function ConnectedServices({ connections, isLoading }: ConnectedServicesProps) {
  if (isLoading) {
    return <div className="h-10 w-full mt-3 mb-3 rounded-2xl bg-white/5 animate-pulse border border-white/5" />;
  }

  const connectedList = connections.filter((c) => c.status === "CONNECTED");

  return (
    <div className="w-full mt-3 mb-3">
      <div className="flex items-center gap-2 px-4 py-2.5 md:px-6 md:py-4 rounded-2xl bg-glass backdrop-blur-glass border border-glass-border shadow-glass-sm">
        {connectedList.length === 0 ? (
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <div className="w-7 h-7 rounded-xl bg-white/5 flex items-center justify-center border border-white/10 shrink-0">
              <Layers className="w-3.5 h-3.5 text-zinc-400" />
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-xs font-bold text-white leading-tight">No connected services</span>
              <span className="text-[10px] text-zinc-400 truncate">Connect Gmail or Classroom to start AI monitoring.</span>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-2 flex-1 min-w-0 overflow-hidden">
            <span className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider shrink-0">
              Connected:
            </span>
            {/* Horizontally scrollable service chips */}
            <div className="flex items-center gap-1.5 overflow-x-auto" style={{ scrollbarWidth: "none" }}>
              {connectedList.map((conn) => {
                const config = platformConfig[conn.platform] || {
                  icon: Mail,
                  name: conn.platform,
                  color: "text-white",
                };
                const Icon = config.icon;
                const email = conn.accountEmail || "";
                // Show short email on mobile: "user@g…" style
                const shortEmail =
                  email.length > 16 ? email.slice(0, 13) + "…" : email;

                return (
                  <div
                    key={conn.platform}
                    className="flex items-center gap-1.5 bg-white/5 shadow-[inset_0_1px_1px_rgba(255,255,255,0.15)] px-3 py-1.5 rounded-lg border border-white/5 shrink-0"
                  >
                    <Icon className={`w-3.5 h-3.5 ${config.color} shrink-0`} />
                    <span className="text-[11px] font-semibold text-white leading-none">
                      {config.name}
                    </span>
                    {shortEmail && (
                      <span className="text-[10px] text-zinc-400 hidden sm:block">
                        {shortEmail}
                      </span>
                    )}
                    <span className="text-[9px] font-extrabold px-1 py-0.5 rounded bg-emerald-500/20 text-emerald-400 leading-none shrink-0">
                      ✓
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        <Link
          href="/integrations"
          className="flex items-center gap-1 px-2 py-1.5 md:px-4 md:py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-[11px] md:text-xs font-semibold transition-all border border-white/10 active:scale-[0.98] shrink-0"
        >
          <span className="hidden md:block">Manage Integrations</span>
          <span className="md:hidden">Manage</span>
          <ArrowRight className="w-3 h-3" />
        </Link>
      </div>
    </div>
  );
}