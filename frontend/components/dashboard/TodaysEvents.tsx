import { EventDTO } from "@/lib/types";
import { StatusDot } from "@/components/ui/StatusDot";
import { GlassBadge } from "@/components/ui/GlassBadge";

const categoryColors = {
  INTERVIEW: { dot: "bg-semantic-blue", badge: "text-semantic-blue border-semantic-blue/30" },
  TASK: { dot: "bg-semantic-green", badge: "text-semantic-green border-semantic-green/30" },
  DEADLINE: { dot: "bg-semantic-amber", badge: "text-semantic-amber border-semantic-amber/30" },
  MEETING: { dot: "bg-semantic-purple", badge: "text-semantic-purple border-semantic-purple/30" },
};

export function TodaysEvents({ events }: { events: EventDTO[] }) {
  return (
    <div className="flex flex-col w-full">
      {events.map((event) => (
        <div key={event.id} className="flex items-center py-2 border-b border-white/10 last:border-0 gap-2">
          <span className="w-14 text-[12px] font-medium text-white/70 shrink-0">{event.time}</span>
          <StatusDot colorClass={categoryColors[event.category].dot} className="shrink-0" />
          <div className="flex-1 flex items-center gap-1.5 min-w-0">
            <span className="text-[13px] font-semibold text-white truncate">{event.title}</span>
            <span className="text-[12px] text-white/45 truncate hidden sm:block">— {event.source}</span>
          </div>
          <GlassBadge colorClass={categoryColors[event.category].badge}>
            {event.category.charAt(0) + event.category.slice(1).toLowerCase()}
          </GlassBadge>
        </div>
      ))}
    </div>
  );
}