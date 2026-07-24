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
        <div key={event.id} className="flex items-center py-3 border-b border-white/10 last:border-0">
          {/* Stacked Date */}
          <div className="flex flex-col items-center justify-center w-12 mr-4">
            <span className="text-lg font-bold text-white leading-none">{event.day}</span>
            <span className="text-xs font-medium text-white/60 uppercase tracking-wider mt-1">{event.month}</span>
          </div>
          
          <StatusDot colorClass={categoryDots[event.category]} className="mr-4" />
          
          <div className="flex-1 flex flex-col justify-center">
            <span className="text-sm font-semibold text-white">{event.company}</span>
            <span className="text-xs text-white/60 mt-0.5">{event.title}</span>
          </div>
          
          <span className="text-sm font-medium text-white/80">{event.time}</span>
        </div>
      ))}
    </div>
  );
}