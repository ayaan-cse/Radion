import { MessageDTO } from "@/lib/types";
import { Mail, MessageCircle, BookOpen } from "lucide-react";
import { StatusDot } from "@/components/ui/StatusDot";

const platformConfig = {
  GMAIL: { icon: Mail, name: "Gmail", color: "text-red-400" },
  WHATSAPP: { icon: MessageCircle, name: "WhatsApp", color: "text-green-400" },
  CLASSROOM: { icon: BookOpen, name: "Classroom", color: "text-yellow-400" },
};

export function RecentMessages({ messages }: { messages: MessageDTO[] }) {
  return (
    <div className="flex flex-col w-full">
      {messages.map((msg) => {
        const config = platformConfig[msg.platform] || { icon: Mail, name: msg.platform, color: "text-white/60" };
        const PlatformIcon = config.icon;

        return (
          <div key={msg.id} className="flex items-center py-2 border-b border-white/10 last:border-0 gap-2">
            {/* Platform icon */}
            <div className="w-7 h-7 rounded-lg bg-white/8 flex items-center justify-center shrink-0">
              <PlatformIcon className={`w-3.5 h-3.5 ${config.color}`} />
            </div>

            {/* Platform name — hidden on very small screens */}
            <span className="w-12 text-[11px] font-medium text-white/60 shrink-0 hidden sm:block">
              {config.name}
            </span>

            {/* Title + summary */}
            <div className="flex-1 flex flex-col min-w-0">
              <span className="text-[13px] font-semibold text-white truncate">{msg.title}</span>
              <span className="text-[11px] text-white/45 truncate mt-0.5">{msg.summary}</span>
            </div>

            <span className="text-[11px] font-medium text-white/40 shrink-0 ml-1">{msg.timestamp}</span>

            {/* Unread dot */}
            {msg.isUnread ? (
              <StatusDot colorClass="bg-semantic-blue shrink-0" />
            ) : (
              <div className="w-2 h-2 shrink-0" />
            )}
          </div>
        );
      })}
    </div>
  );
}