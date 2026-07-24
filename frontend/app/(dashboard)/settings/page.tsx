"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { useDashboard } from "@/hooks/useDashboard";
import { Activity, Clock, CheckCircle, Zap } from "lucide-react";

export default function SettingsAnalyticsPage() {
  const { data, isLoading } = useDashboard();

  if (isLoading || !data) return <div className="p-8 text-white/60">Loading analytics...</div>;

  const { analytics } = data;

  return (
    <div className="flex-1 flex flex-col pt-4 overflow-y-auto custom-scrollbar pr-4">
      <h1 className="text-2xl font-bold text-white tracking-tight mb-2">Analytics & History</h1>
      <p className="text-sm text-white/60 mb-8">Review your AI processing statistics and system preferences.</p>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <GlassCard className="p-5 flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-semantic-blue/20 flex items-center justify-center border border-semantic-blue/30">
            <Zap className="w-5 h-5 text-semantic-blue" />
          </div>
          <div>
            <p className="text-xs text-white/60 font-medium uppercase tracking-wider">Events Automated</p>
            <p className="text-2xl font-bold text-white">{analytics.totalEventsAutomated}</p>
          </div>
        </GlassCard>
        
        <GlassCard className="p-5 flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-semantic-green/20 flex items-center justify-center border border-semantic-green/30">
            <Clock className="w-5 h-5 text-semantic-green" />
          </div>
          <div>
            <p className="text-xs text-white/60 font-medium uppercase tracking-wider">Hours Saved</p>
            <p className="text-2xl font-bold text-white">{analytics.hoursSaved}h</p>
          </div>
        </GlassCard>

        <GlassCard className="p-5 flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-semantic-amber/20 flex items-center justify-center border border-semantic-amber/30">
            <CheckCircle className="w-5 h-5 text-semantic-amber" />
          </div>
          <div>
            <p className="text-xs text-white/60 font-medium uppercase tracking-wider">Pending Tasks</p>
            <p className="text-2xl font-bold text-white">{analytics.tasksPending}</p>
          </div>
        </GlassCard>

        <GlassCard className="p-5 flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-semantic-purple/20 flex items-center justify-center border border-semantic-purple/30">
            <Activity className="w-5 h-5 text-semantic-purple" />
          </div>
          <div>
            <p className="text-xs text-white/60 font-medium uppercase tracking-wider">AI Confidence</p>
            <p className="text-2xl font-bold text-white">{analytics.averageAiConfidence * 100}%</p>
          </div>
        </GlassCard>
      </div>

      {/* AI Processing Logs */}
      <GlassCard className="flex-1 flex flex-col p-6 min-h-[400px]">
        <h2 className="text-white font-bold text-[17px] tracking-wide mb-4">Recent AI Processing Logs</h2>
        <div className="flex flex-col w-full">
          {data.recentMessages.map((msg) => (
            <div key={msg.id} className="flex items-center py-3 border-b border-white/10 last:border-0">
              <span className="w-24 text-sm font-medium text-white/80">{msg.platform}</span>
              <div className="flex-1 flex flex-col justify-center">
                <span className="text-sm font-semibold text-white">{msg.title}</span>
                <span className="text-xs text-white/50 truncate max-w-2xl mt-0.5">{msg.summary}</span>
              </div>
              <span className="text-xs font-medium text-white/40">{msg.timestamp}</span>
            </div>
          ))}
        </div>
      </GlassCard>
    </div>
  );
}