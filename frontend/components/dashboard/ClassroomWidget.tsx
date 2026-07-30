import { GlassCard } from "@/components/ui/GlassCard";
import { GlassBadge } from "@/components/ui/GlassBadge";
import { BookOpen, AlertCircle, Megaphone, Calendar } from "lucide-react";
import { ClassroomAssignmentDTO, ClassroomAnnouncementDTO } from "@/lib/types";

export function ClassroomWidget({
  upcomingAssignments,
  overdueAssignments,
  recentAnnouncements,
}: {
  upcomingAssignments?: ClassroomAssignmentDTO[];
  overdueAssignments?: ClassroomAssignmentDTO[];
  recentAnnouncements?: ClassroomAnnouncementDTO[];
}) {
  const hasData =
    (upcomingAssignments && upcomingAssignments.length > 0) ||
    (overdueAssignments && overdueAssignments.length > 0) ||
    (recentAnnouncements && recentAnnouncements.length > 0);

  if (!hasData) {
    return null;
  }

  return (
    <GlassCard className="col-span-1 border-emerald-900/50 bg-black/40 shadow-xl overflow-hidden relative">
      <div className="absolute inset-0 bg-gradient-to-br from-emerald-900/10 to-transparent pointer-events-none" />
      <div className="pb-3 p-4 relative z-10 border-b border-emerald-900/30">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold flex items-center gap-2 text-emerald-50">
            <BookOpen className="h-5 w-5 text-emerald-400" />
            Classroom Activity
          </h2>
          <GlassBadge colorClass="text-emerald-400" className="bg-emerald-500/10 border-emerald-500/20">
            Live Sync
          </GlassBadge>
        </div>
      </div>
      <div className="p-4 space-y-6 relative z-10">
        
        {/* Overdue */}
        {overdueAssignments && overdueAssignments.length > 0 && (
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-rose-400 flex items-center gap-2">
              <AlertCircle className="h-4 w-4" />
              Overdue
            </h3>
            <div className="space-y-2">
              {overdueAssignments.map((a) => (
                <div key={a.id} className="p-3 rounded-lg bg-rose-500/5 border border-rose-500/20 hover:bg-rose-500/10 transition-colors">
                  <p className="text-sm font-medium text-rose-100">{a.title}</p>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-xs text-rose-300/70">{a.courseName}</span>
                    <span className="text-xs text-rose-400 font-medium">Due {a.dueDate}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Upcoming */}
        {upcomingAssignments && upcomingAssignments.length > 0 && (
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-emerald-400 flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Upcoming
            </h3>
            <div className="space-y-2">
              {upcomingAssignments.map((a) => (
                <div key={a.id} className="p-3 rounded-lg bg-emerald-500/5 border border-emerald-500/20 hover:bg-emerald-500/10 transition-colors">
                  <p className="text-sm font-medium text-emerald-100">{a.title}</p>
                  <div className="flex items-center justify-between mt-2">
                    <span className="text-xs text-emerald-300/70">{a.courseName}</span>
                    <span className="text-xs text-emerald-400 font-medium">Due {a.dueDate}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Announcements */}
        {recentAnnouncements && recentAnnouncements.length > 0 && (
          <div className="space-y-3 pt-2 border-t border-emerald-900/30">
            <h3 className="text-sm font-semibold text-slate-300 flex items-center gap-2">
              <Megaphone className="h-4 w-4" />
              Recent Announcements
            </h3>
            <div className="space-y-2">
              {recentAnnouncements.map((ca) => (
                <div key={ca.id} className="p-3 rounded-lg bg-slate-800/50 border border-slate-700/50">
                  <p className="text-xs text-slate-400 mb-1">{ca.courseName}</p>
                  <p className="text-sm text-slate-200 line-clamp-2 leading-relaxed">{ca.text}</p>
                  <p className="text-[10px] text-slate-500 mt-2">{ca.postedAt}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </GlassCard>
  );
}
