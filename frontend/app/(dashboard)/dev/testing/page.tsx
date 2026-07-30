"use client";

import { useState } from "react";
import { apiClient } from "@/lib/api";
import { DryRunResponseDTO, DryRunResultItemDTO, BusinessCommandDTO } from "@/lib/types";
import { GlassCard } from "@/components/ui/GlassCard";
import { motion, AnimatePresence } from "framer-motion";
import { 
  Play, 
  Loader2, 
  ShieldCheck, 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  Sparkles, 
  Mail, 
  Filter, 
  Activity,
  Quote,
  Database,
  Calendar,
  CheckSquare,
  FileText,
  Bell,
  Clock,
  Compass,
  ArrowRight,
  ChevronDown,
  ChevronUp,
  Link as LinkIcon
} from "lucide-react";
import { cn } from "@/lib/utils";

const LIMIT_OPTIONS = [
  { label: "Latest 10 Emails", value: 10 },
  { label: "Latest 25 Emails", value: 25 },
  { label: "Latest 50 Emails", value: 50 },
  { label: "Latest 100 Emails", value: 100 },
  { label: "Latest 200 Emails", value: 200 },
  { label: "Latest 500 Emails", value: 500 },
  { label: "All Emails", value: -1 },
];

export default function DeveloperTestingPage() {
  const [selectedLimit, setSelectedLimit] = useState<number>(50);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<DryRunResponseDTO | null>(null);
  const [expandedRows, setExpandedRows] = useState<Record<string, boolean>>({});

  const toggleRow = (messageId: string) => {
    setExpandedRows(prev => ({ ...prev, [messageId]: !prev[messageId] }));
  };

  const handleStartTest = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiClient.runDryRunClassification(selectedLimit);
      setData(result);
      // Auto-expand rows that have commands or errors
      const initialExpanded: Record<string, boolean> = {};
      result.results.forEach(res => {
        if (res.commands.length > 0 || res.error || res.uncertainty) {
          initialExpanded[res.messageId] = true;
        }
      });
      setExpandedRows(initialExpanded);
    } catch (err: any) {
      setError(err.message || "An error occurred while running the dry-run test.");
    } finally {
      setLoading(false);
    }
  };

  const getCommandIcon = (commandType: string) => {
    switch (commandType) {
      case "REGISTER_OPPORTUNITY":
        return <Compass className="w-3.5 h-3.5 text-blue-400" />;
      case "ADVANCE_OPPORTUNITY_STAGE":
        return <ArrowRight className="w-3.5 h-3.5 text-indigo-400" />;
      case "SCHEDULE_INTERVIEW":
        return <Calendar className="w-3.5 h-3.5 text-purple-400" />;
      case "SCHEDULE_ASSESSMENT":
        return <FileText className="w-3.5 h-3.5 text-amber-400" />;
      case "ASSIGN_ACTION_ITEM":
        return <CheckSquare className="w-3.5 h-3.5 text-emerald-400" />;
      case "COMPLETE_ACTION_ITEM":
        return <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />;
      case "ANNOUNCE_EVENT":
        return <Bell className="w-3.5 h-3.5 text-rose-400" />;
      default:
        return <Database className="w-3.5 h-3.5 text-cyan-400" />;
    }
  };

  const getCommandBadge = (commandType: string) => {
    switch (commandType) {
      case "REGISTER_OPPORTUNITY":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">REGISTER OPPORTUNITY</span>;
      case "ADVANCE_OPPORTUNITY_STAGE":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">ADVANCE STAGE</span>;
      case "SCHEDULE_INTERVIEW":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30">SCHEDULE INTERVIEW</span>;
      case "SCHEDULE_ASSESSMENT":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">SCHEDULE ASSESSMENT</span>;
      case "ASSIGN_ACTION_ITEM":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">ASSIGN TASK</span>;
      case "COMPLETE_ACTION_ITEM":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">COMPLETE TASK</span>;
      case "ANNOUNCE_EVENT":
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30">ANNOUNCE EVENT</span>;
      default:
        return <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-white/10 text-white/70 border border-white/20">{commandType}</span>;
    }
  };

  return (
    <div className="flex flex-col h-full overflow-y-auto pr-2 gap-8 text-white pb-12">
      {/* Header Section */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-glass backdrop-blur-glass p-6 rounded-glass border border-glass-border shadow-glass">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight bg-gradient-to-r from-white via-white/90 to-white/70 bg-clip-text text-transparent">
              AI Journey Reasoning Sandbox
            </h1>
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 shadow-[0_0_12px_rgba(16,185,129,0.15)]">
              <ShieldCheck className="w-3.5 h-3.5" /> Read-Only Mode 🔒
            </span>
          </div>
          <p className="text-sm text-white/70 max-w-2xl">
            Execute dry-run entity reasoning against stored Gmail messages. Evaluates how emails impact the student&apos;s evolving world state (Opportunities, Tasks, Events) via semantic Business Commands with guaranteed zero database side effects.
          </p>
        </div>
      </div>

      {/* Dry Run Controls Section */}
      <GlassCard className="p-6">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-3 flex-1">
            <label className="text-sm font-medium text-white/80 flex items-center gap-2">
              <Filter className="w-4 h-4 text-semantic-blue" /> Select Dry Run Batch Size
            </label>
            <div className="flex flex-wrap gap-2">
              {LIMIT_OPTIONS.map((opt) => {
                const isSelected = selectedLimit === opt.value;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setSelectedLimit(opt.value)}
                    disabled={loading}
                    className={cn(
                      "px-4 py-2 rounded-xl text-xs font-semibold transition-all duration-200 border outline-none focus-visible:ring-2 focus-visible:ring-white/30",
                      isSelected
                        ? "bg-gradient-to-r from-semantic-blue to-blue-600 text-white border-blue-400/50 shadow-[0_0_16px_rgba(59,130,246,0.4)] scale-[1.02]"
                        : "bg-white/5 text-white/70 border-white/10 hover:bg-white/10 hover:text-white"
                    )}
                  >
                    {opt.label}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="flex items-center gap-4">
            <button
              type="button"
              onClick={handleStartTest}
              disabled={loading}
              className="flex items-center justify-center gap-2.5 px-6 py-3 rounded-xl font-semibold text-sm bg-gradient-to-r from-emerald-500 to-teal-600 text-white shadow-[0_0_20px_rgba(16,185,129,0.3)] hover:shadow-[0_0_28px_rgba(16,185,129,0.5)] hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:pointer-events-none min-w-[180px]"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Reasoning...
                </>
              ) : (
                <>
                  <Play className="w-4 h-4 fill-current" /> Start Dry Run Test
                </>
              )}
            </button>
          </div>
        </div>
      </GlassCard>

      {/* Error Message */}
      <AnimatePresence>
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="p-4 rounded-2xl bg-rose-500/20 border border-rose-500/30 text-rose-200 flex items-center gap-3"
          >
            <AlertTriangle className="w-5 h-5 text-rose-400 flex-shrink-0" />
            <span className="text-sm font-medium">{error}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Summary Statistics & Results */}
      {data && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="space-y-8"
        >
          {/* Summary Stats Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
            <GlassCard className="p-4 flex flex-col justify-between border-white/20">
              <span className="text-xs font-medium text-white/60 flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-white/80" /> Total Evaluated
              </span>
              <span className="text-2xl font-bold mt-2">{data.summary.totalEmails}</span>
            </GlassCard>

            <GlassCard className="p-4 flex flex-col justify-between border-emerald-500/30 bg-emerald-500/5">
              <span className="text-xs font-medium text-emerald-400 flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" /> Impactful Emails
              </span>
              <span className="text-2xl font-bold text-emerald-300 mt-2">{data.summary.impactfulCount}</span>
            </GlassCard>

            <GlassCard className="p-4 flex flex-col justify-between border-white/10 bg-white/[0.02]">
              <span className="text-xs font-medium text-white/50 flex items-center gap-1.5">
                <XCircle className="w-3.5 h-3.5" /> Ignored (No Impact)
              </span>
              <span className="text-2xl font-bold text-white/70 mt-2">{data.summary.ignoredCount}</span>
            </GlassCard>

            <GlassCard className="p-4 flex flex-col justify-between border-amber-500/30 bg-amber-500/5">
              <span className="text-xs font-medium text-amber-400 flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5" /> Uncertain
              </span>
              <span className="text-2xl font-bold text-amber-300 mt-2">{data.summary.uncertainCount}</span>
            </GlassCard>

            <GlassCard className="p-4 flex flex-col justify-between border-rose-500/30 bg-rose-500/5">
              <span className="text-xs font-medium text-rose-400 flex items-center gap-1.5">
                <AlertTriangle className="w-3.5 h-3.5" /> Errors
              </span>
              <span className="text-2xl font-bold text-rose-300 mt-2">{data.summary.errorCount}</span>
            </GlassCard>

            <GlassCard className="p-4 flex flex-col justify-between border-purple-500/30 bg-purple-500/5 shadow-[0_0_15px_rgba(168,85,247,0.15)]">
              <span className="text-xs font-medium text-purple-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5" /> Predicted Commands
              </span>
              <span className="text-2xl font-bold text-purple-300 mt-2">{data.summary.totalCommandsPredicted}</span>
            </GlassCard>
          </div>

          {/* Detailed Evaluation Table */}
          <GlassCard className="p-6 overflow-hidden">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-white/90 flex items-center gap-2">
                <Activity className="w-5 h-5 text-semantic-blue" /> Entity Reasoning Results
              </h2>
              <span className="text-xs text-white/50">
                Showing {data.results.length} evaluated messages
              </span>
            </div>

            {data.results.length === 0 ? (
              <div className="text-center py-12 text-white/60 space-y-2">
                <Mail className="w-10 h-10 mx-auto text-white/30" />
                <p className="font-medium">No Gmail messages found in database.</p>
                <p className="text-xs text-white/40">Try connecting Gmail or syncing messages in the Integrations page first.</p>
              </div>
            ) : (
              <div className="overflow-x-auto -mx-6 px-6">
                <table className="w-full text-left border-collapse min-w-[1000px]">
                  <thead>
                    <tr className="border-b border-white/10 text-xs uppercase tracking-wider text-white/50 font-semibold">
                      <th className="pb-3 pr-4 w-10"></th>
                      <th className="pb-3 pr-4">Sender & Subject</th>
                      <th className="pb-3 pr-4">Impact Status</th>
                      <th className="pb-3 pr-4">AI Email Summary</th>
                      <th className="pb-3 pr-4 text-center">Duration</th>
                      <th className="pb-3 pr-4 text-center">Commands</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-sm">
                    {data.results.map((res: DryRunResultItemDTO) => {
                      const isExpanded = !!expandedRows[res.messageId];
                      const hasCommands = res.commands && res.commands.length > 0;
                      return (
                        <>
                          <tr 
                            key={res.messageId} 
                            onClick={() => toggleRow(res.messageId)}
                            className={cn(
                              "hover:bg-white/[0.03] transition-colors cursor-pointer group",
                              isExpanded ? "bg-white/[0.04]" : ""
                            )}
                          >
                            <td className="py-4 pr-2 text-white/40 group-hover:text-white/80">
                              {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                            </td>
                            <td className="py-4 pr-4 max-w-[300px]">
                              <div className="font-semibold text-white/95 truncate" title={res.sender}>
                                {res.sender}
                              </div>
                              <div className="text-xs text-white/70 truncate mt-0.5" title={res.subject}>
                                {res.subject}
                              </div>
                            </td>
                            <td className="py-4 pr-4 whitespace-nowrap">
                              {res.error ? (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">
                                  <AlertTriangle className="w-3.5 h-3.5" /> ERROR
                                </span>
                              ) : res.uncertainty ? (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400 border border-amber-500/30">
                                  <AlertTriangle className="w-3.5 h-3.5" /> UNCERTAIN
                                </span>
                              ) : res.hasJourneyImpact ? (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 shadow-[0_0_10px_rgba(16,185,129,0.2)]">
                                  <CheckCircle2 className="w-3.5 h-3.5" /> IMPACTFUL
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-white/5 text-white/50 border border-white/10">
                                  <XCircle className="w-3.5 h-3.5" /> IGNORED
                                </span>
                              )}
                            </td>
                            <td className="py-4 pr-4 text-white/80 max-w-[400px]">
                              <p className="line-clamp-2 text-xs leading-relaxed font-normal text-white/75">
                                {res.emailSummary || (res.errorMessage ? `Error: ${res.errorMessage}` : "No summary provided.")}
                              </p>
                              {res.uncertaintyReason && (
                                <p className="text-[11px] text-amber-300/80 mt-1 italic">
                                  Reason: {res.uncertaintyReason}
                                </p>
                              )}
                            </td>
                            <td className="py-4 pr-4 text-center whitespace-nowrap text-xs font-mono text-white/70">
                              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-white/5 border border-white/10">
                                <Clock className="w-3.5 h-3.5 text-cyan-400" />
                                {res.processingDurationMs !== undefined ? `${res.processingDurationMs}ms` : 'N/A'}
                              </span>
                            </td>
                            <td className="py-4 pr-4 text-center">
                              {hasCommands ? (
                                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-purple-500/20 text-purple-300 border border-purple-500/40 shadow-[0_0_12px_rgba(168,85,247,0.25)]">
                                  <Sparkles className="w-3.5 h-3.5" /> {res.commands.length} {res.commands.length === 1 ? 'Command' : 'Commands'}
                                </span>
                              ) : (
                                <span className="text-xs text-white/30 font-medium">None</span>
                              )}
                            </td>
                          </tr>

                          {/* Expanded Details Row for Business Commands & Evidence Quotes */}
                          {isExpanded && (
                            <tr className="bg-black/20 border-b border-white/10">
                              <td></td>
                              <td colSpan={5} className="py-4 pr-6 pb-6 space-y-4">
                                <div className="p-4 rounded-xl bg-white/[0.03] border border-white/10 space-y-4">
                                  <div className="flex items-center justify-between border-b border-white/10 pb-2.5">
                                    <h4 className="text-xs font-semibold uppercase tracking-wider text-white/70 flex items-center gap-2">
                                      <Database className="w-3.5 h-3.5 text-purple-400" /> Predicted Business Commands & Evidence
                                    </h4>
                                    <span className="text-[11px] text-white/40 font-mono">ID: {res.messageId}</span>
                                  </div>

                                  {!hasCommands ? (
                                    <p className="text-xs text-white/50 italic py-2">
                                      {res.hasJourneyImpact 
                                        ? "Email marked as impactful, but no explicit business commands were emitted." 
                                        : "No business commands emitted. This evidence does not alter any academic or placement state."}
                                    </p>
                                  ) : (
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                      {res.commands.map((cmd: BusinessCommandDTO, idx: number) => (
                                        <div 
                                          key={idx}
                                          className="p-3.5 rounded-lg bg-white/[0.04] border border-white/10 space-y-2.5 flex flex-col justify-between"
                                        >
                                          <div className="flex items-center justify-between gap-2">
                                            <div className="flex items-center gap-2 font-semibold text-xs text-white/90">
                                              {getCommandIcon(cmd.commandType)}
                                              <span>{cmd.commandType}</span>
                                            </div>
                                            {getCommandBadge(cmd.commandType)}
                                          </div>

                                          {/* Command Details */}
                                          <div className="text-xs text-white/80 space-y-1 pl-1 border-l-2 border-white/10 py-1">
                                            {cmd.companyName && (
                                              <div><span className="text-white/40">Company:</span> <span className="font-medium text-white">{cmd.companyName}</span></div>
                                            )}
                                            {cmd.stage && (
                                              <div><span className="text-white/40">Stage:</span> <span className="font-medium text-blue-300">{cmd.stage}</span></div>
                                            )}
                                            {cmd.role && (
                                              <div><span className="text-white/40">Role:</span> <span className="font-medium text-white">{cmd.role}</span></div>
                                            )}
                                            {cmd.ctc && (
                                              <div><span className="text-white/40">CTC/Compensation:</span> <span className="font-medium text-emerald-300">{cmd.ctc}</span></div>
                                            )}
                                            {cmd.title && (
                                              <div><span className="text-white/40">Title:</span> <span className="font-medium text-white">{cmd.title}</span></div>
                                            )}
                                            {cmd.scheduledTime && (
                                              <div className="flex items-center gap-1"><Clock className="w-3 h-3 text-purple-400" /><span className="text-white/40">Scheduled Time:</span> <span className="font-medium text-purple-300">{cmd.scheduledTime}</span></div>
                                            )}
                                            {cmd.dueDate && (
                                              <div className="flex items-center gap-1"><Clock className="w-3 h-3 text-amber-400" /><span className="text-white/40">Due Date:</span> <span className="font-medium text-amber-300">{cmd.dueDate}</span></div>
                                            )}
                                            {cmd.meetingLinkOrUrl && (
                                              <div className="flex items-center gap-1 truncate"><LinkIcon className="w-3 h-3 text-cyan-400" /><span className="text-white/40">Link:</span> <span className="font-medium text-cyan-300 truncate">{cmd.meetingLinkOrUrl}</span></div>
                                            )}
                                            {cmd.description && (
                                              <div className="text-white/70 text-[11px] mt-1">{cmd.description}</div>
                                            )}
                                          </div>

                                          {/* Verbatim Evidence Quote */}
                                          {cmd.evidenceQuote ? (
                                            <div className="pt-2 border-t border-white/5">
                                              <div className="flex items-start gap-1.5 text-[11px] text-white/60 bg-black/30 p-2 rounded border border-white/5 italic">
                                                <Quote className="w-3.5 h-3.5 text-purple-400 flex-shrink-0 mt-0.5" />
                                                <span className="line-clamp-3">&ldquo;{cmd.evidenceQuote}&rdquo;</span>
                                              </div>
                                            </div>
                                          ) : (
                                            <div className="pt-1 text-[10px] text-white/30 italic">No direct quote extracted</div>
                                          )}

                                          {/* Execution & Calendar Sync Results */}
                                          {(cmd.executionResult || cmd.calendarSyncResult || cmd.executionError) && (
                                            <div className="pt-2 mt-2 border-t border-white/10 space-y-1.5 text-xs">
                                              {cmd.executionResult && (
                                                <div className="flex items-start gap-1.5 p-2 rounded bg-blue-500/10 border border-blue-500/20 text-blue-200 font-mono text-[11px]">
                                                  <Database className="w-3.5 h-3.5 text-blue-400 flex-shrink-0 mt-0.5" />
                                                  <div className="w-full">
                                                    <span className="font-semibold text-blue-300 block">DB Status / Result:</span>
                                                    <span className="text-white/80">{cmd.executionResult}</span>
                                                  </div>
                                                </div>
                                              )}
                                              {cmd.executionError && (
                                                <div className="flex items-start gap-1.5 p-2 rounded bg-red-500/10 border border-red-500/20 text-red-200 font-mono text-[11px]">
                                                  <AlertTriangle className="w-3.5 h-3.5 text-red-400 flex-shrink-0 mt-0.5" />
                                                  <div className="w-full">
                                                    <span className="font-semibold text-red-300 block">DB Error:</span>
                                                    <span className="text-white/80">{cmd.executionError}</span>
                                                  </div>
                                                </div>
                                              )}
                                              {cmd.calendarSyncResult && (
                                                <div className="flex items-start gap-1.5 p-2 rounded bg-emerald-500/10 border border-emerald-500/20 text-emerald-200 font-mono text-[11px]">
                                                  <Calendar className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0 mt-0.5" />
                                                  <div className="w-full">
                                                    <div className="flex items-center justify-between">
                                                      <span className="font-semibold text-emerald-300">Calendar Sync Status:</span>
                                                      {cmd.calendarEventId && (
                                                        <span className="text-[10px] bg-emerald-500/20 px-1.5 py-0.5 rounded border border-emerald-500/30 text-emerald-300">
                                                          CalendarEventID: {cmd.calendarEventId}
                                                        </span>
                                                      )}
                                                    </div>
                                                    <div className="mt-0.5 text-white/80">{cmd.calendarSyncResult}</div>
                                                  </div>
                                                </div>
                                              )}
                                            </div>
                                          )}
                                        </div>
                                      ))}
                                    </div>
                                  )}
                                </div>
                              </td>
                            </tr>
                          )}
                        </>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </GlassCard>
        </motion.div>
      )}
    </div>
  );
}
