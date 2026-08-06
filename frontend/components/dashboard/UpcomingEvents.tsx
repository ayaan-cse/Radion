import { UpcomingEventDTO } from "@/lib/types";
import { StatusDot } from "@/components/ui/StatusDot";

const categoryDots = {
  INTERVIEW: "bg-semantic-blue",
  TASK: "bg-semantic-green",
  DEADLINE: "bg-semantic-amber",
  MEETING: "bg-semantic-purple",
};

export function UpcomingEvents({ events }: { events: UpcomingEventDTO[] }) {
  return (
    <div className="flex flex-col w-full">
      {events.map((event) => (
        <div key={event.id} className="flex items-center py-2 border-b border-white/10 last:border-0 gap-2">
          {/* Stacked Date — compact */}
          <div className="flex flex-col items-center justify-center w-9 shrink-0">
            <span className="text-[15px] font-bold text-white leading-none">{event.day}</span>
            <span className="text-[9px] font-medium text-white/55 uppercase tracking-wider">{event.month}</span>
          </div>

          <StatusDot colorClass={categoryDots[event.category]} className="shrink-0" />

          <div className="flex-1 flex flex-col min-w-0">
            <span className="text-[13px] font-semibold text-white truncate">{event.company}</span>
            <span className="text-[11px] text-white/55 truncate">{event.title}</span>
          </div>

          <span className="text-[12px] font-medium text-white/70 shrink-0 ml-1">{event.time}</span>
        </div>
      ))}
    </div>
  );
}