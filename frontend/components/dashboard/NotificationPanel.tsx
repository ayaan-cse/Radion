import { motion } from "framer-motion";
import { NotificationDTO } from "@/lib/types";
import { CheckCircle2 } from "lucide-react";

interface NotificationPanelProps {
  notifications: NotificationDTO[];
  onMarkAsRead: (id: string) => void;
}

export function NotificationPanel({ notifications, onMarkAsRead }: NotificationPanelProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 10, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: 10, scale: 0.95 }}
      transition={{ duration: 0.2, ease: "easeOut" }}
      className="absolute top-14 right-0 w-80 max-h-96 overflow-y-auto custom-scrollbar rounded-glass bg-glass backdrop-blur-glass border border-glass-border shadow-glass p-2 z-50"
    >
      <div className="px-3 py-2 border-b border-white/10 mb-2">
        <h3 className="text-sm font-semibold text-white">Notifications</h3>
      </div>
      
      {notifications.length === 0 ? (
        <div className="p-4 text-center text-sm text-white/50">No new notifications</div>
      ) : (
        <div className="flex flex-col gap-1">
          {notifications.map((notif) => (
            <div 
              key={notif.id} 
              className={`p-3 rounded-xl transition-colors ${notif.isRead ? 'opacity-60' : 'bg-white/5 hover:bg-white/10'}`}
            >
              <div className="flex justify-between items-start mb-1">
                <span className="text-sm font-medium text-white">{notif.title}</span>
                {!notif.isRead && (
                  <button onClick={() => onMarkAsRead(notif.id)} className="text-white/40 hover:text-semantic-green transition-colors">
                    <CheckCircle2 className="w-4 h-4" />
                  </button>
                )}
              </div>
              <p className="text-xs text-white/70 leading-relaxed mb-2">{notif.content}</p>
              <span className="text-[10px] text-white/40 font-medium">{notif.timestamp}</span>
            </div>
          ))}
        </div>
      )}
    </motion.div>
  );
}