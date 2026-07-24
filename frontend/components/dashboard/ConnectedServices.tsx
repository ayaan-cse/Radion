import { ConnectionDTO } from "@/lib/types";
import { Mail, MessageCircle, BookOpen } from "lucide-react";

const platformConfig = {
  GMAIL: { icon: Mail, name: "Gmail", color: "text-red-400" },
  WHATSAPP: { icon: MessageCircle, name: "WhatsApp", color: "text-green-400" },
  CLASSROOM: { icon: BookOpen, name: "Classroom", color: "text-yellow-400" },
};

export function ConnectedServices({ connections, isLoading }: { connections: ConnectionDTO[], isLoading: boolean }) {
  if (isLoading) return <div className="h-[60px] w-full mt-4 mb-6" />; // Spacer

  return (
    <div className="flex justify-end w-full mt-4 mb-6">
      <div className="flex items-center gap-8 px-6 py-3 rounded-full bg-glass backdrop-blur-glass border border-glass-border shadow-glass-sm">
        {connections.map((conn) => {
          const config = platformConfig[conn.platform];
          const Icon = config.icon;
          return (
            <div key={conn.platform} className="flex items-center gap-3">
              <div className="w-6 h-6 rounded-md bg-white/10 flex items-center justify-center">
                <Icon className={`w-3.5 h-3.5 ${config.color}`} />
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-medium text-white leading-tight">{config.name}</span>
                <span className={`text-[10px] font-semibold leading-tight tracking-wide uppercase ${conn.status === 'CONNECTED' ? 'text-emerald-400' : 'text-red-400'}`}>
                  {conn.status.toLowerCase()}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}