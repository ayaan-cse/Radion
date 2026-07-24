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
        const PlatformIcon = platformConfig[msg.platform].icon;
        
        return (
          <div key={msg.id} className="flex items-center py-3 border-b border-white/10 last:border-0">
            {/* Platform Icon matching screenshot style */}
            <div className="w-8 h-8 rounded-md bg-white/10 flex items-center justify-center mr-3">
              <PlatformIcon className={`w-4 h-4 ${platformConfig[msg.platform].color}`} />
            </div>
            
            <span className="w-24 text-sm font-medium text-white/80">{platformConfig[msg.platform].name}</span>
            
            <div className="flex-1 flex flex-col justify-center">
              <span className="text-sm font-semibold text-white">{msg.title}</span>
              <span className="text-xs text-white/50 truncate max-w-md mt-0.5">{msg.summary}</span>
            </div>
            
            <span className="text-xs font-medium text-white/40 mr-4">{msg.timestamp}</span>
            
            {/* Unread Dot */}
            {msg.isUnread ? (
              <StatusDot colorClass="bg-semantic-blue" />
            ) : (
              <div className="w-2 h-2" /> /* Spacer for alignment */
            )}
          </div>
        );
      })}
    </div>
  );
}