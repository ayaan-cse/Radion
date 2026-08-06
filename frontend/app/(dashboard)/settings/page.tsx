"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { useDashboard } from "@/hooks/useDashboard";
import { Activity, Clock, CheckCircle, Zap } from "lucide-react";

export default function SettingsAnalyticsPage() {
  const { data, isLoading } = useDashboard();

  if (isLoading || !data) {
    return <div className="p-4 text-white/60 text-sm">Loading analytics...</div>;
  }

  const { analytics } = data;

  const stats = [
    {
      label: "Events Automated",
      value: analytics.totalEventsAutomated,
      suffix: "",
      icon: Zap,
      color: "text-semantic-blue",
      bg: "bg-semantic-blue/20",
      border: "border-semantic-blue/30",
    },
    {
      label: "Hours Saved",
      value: analytics.hoursSaved,
      suffix: "h",
      icon: Clock,
      color: "text-semantic-green",
      bg: "bg-semantic-green/20",
      border: "border-semantic-green/30",
    },
    {
      label: "Pending Tasks",
      value: analytics.tasksPending,
      suffix: "",
      icon: CheckCircle,
      color: "text-semantic-amber",
      bg: "bg-semantic-amber/20",
      border: "border-semantic-amber/30",
    },
    {
      label: "AI Confidence",
      value: Math.round(analytics.averageAiConfidence * 100),
      suffix: "%",
      icon: Activity,
      color: "text-semantic-purple",
      bg: "bg-semantic-purple/20",
      border: "border-semantic-purple/30",
    },
  ];

  return (
    <div className="flex-1 flex flex-col pt-2 md:pt-4 overflow-y-auto custom-scrollbar pr-2 md:pr-4">
      <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight mb-1">
        Analytics &amp; History
      </h1>
      <p className="text-[11px] md:text-sm text-white/60 mb-4 md:mb-8">
        Review your AI processing statistics.
      </p>

      {/* Stats Grid — 2×2 on mobile, 4-col on lg */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 md:gap-6 mb-4 md:mb-8">
        {stats.map(({ label, value, suffix, icon: Icon, color, bg, border }) => (
          <GlassCard key={label} className="p-3 md:p-5 flex items-center gap-2.5 md:gap-4">
            <div
              className={`w-8 h-8 md:w-10 md:h-10 rounded-xl ${bg} flex items-center justify-center border ${border} shrink-0`}
            >
              <Icon className={`w-4 h-4 ${color}`} />
            </div>
            <div className="min-w-0">
              <p className="text-[9px] md:text-[10px] text-white/60 font-medium uppercase tracking-wider leading-tight">
                {label}
              </p>
              <p className="text-xl md:text-2xl font-bold text-white leading-tight">
                {value}
                {suffix}
              </p>
            </div>
          </GlassCard>
        ))}
      </div>

      {/* AI Processing Logs */}
      <GlassCard className="flex flex-col p-4 md:p-6">
        <h2 className="text-white font-bold text-[14px] md:text-[17px] tracking-wide mb-3">
          Recent AI Processing Logs
        </h2>
        <div className="flex flex-col w-full">
          {data.recentMessages.length === 0 ? (
            <p className="text-xs text-white/40 py-4 text-center">No logs yet.</p>
          ) : (
            data.recentMessages.map((msg) => (
              <div
                key={msg.id}
                className="flex items-center py-2 border-b border-white/10 last:border-0 gap-2"
              >
                <span className="text-[10px] font-medium text-white/60 uppercase tracking-wider w-14 shrink-0">
                  {msg.platform}
                </span>
                <div className="flex-1 flex flex-col min-w-0">
                  <span className="text-[13px] font-semibold text-white truncate">{msg.title}</span>
                  <span className="text-[11px] text-white/50 truncate mt-0.5">{msg.summary}</span>
                </div>
                <span className="text-[10px] font-medium text-white/40 shrink-0 ml-2">
                  {msg.timestamp}
                </span>
              </div>
            ))
          )}
        </div>
      </GlassCard>
    </div>
  );
}