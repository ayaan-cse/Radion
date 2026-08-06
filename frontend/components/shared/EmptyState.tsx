import { cn } from "@/lib/utils";

export function EmptyState({ message, className }: { message: string; className?: string }) {
  return (
    <div className={cn("flex w-full items-center justify-center py-6 opacity-50", className)}>
      <p className="text-xs md:text-sm text-white/60 font-medium tracking-wide">{message}</p>
    </div>
  );
}