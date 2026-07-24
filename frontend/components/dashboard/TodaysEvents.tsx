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
        <div key={event.id} className="flex items-center py-3 border-b border-white/10 last:border-0">
          <span className="w-20 text-sm font-medium text-white/80">{event.time}</span>
          <StatusDot colorClass={categoryColors[event.category].dot} className="mr-4" />
          <div className="flex-1 flex items-center gap-2">
            <span className="text-sm font-semibold text-white">{event.title}</span>
            <span className="text-sm text-white/50">— {event.source}</span>
          </div>
          <GlassBadge colorClass={categoryColors[event.category].badge}>
            {event.category.charAt(0) + event.category.slice(1).toLowerCase()}
          </GlassBadge>
        </div>
      ))}
    </div>
  );
}