import { cn } from "@/lib/utils";

export function EmptyState({ message, className }: { message: string; className?: string }) {
  return (
    <div className={cn("flex h-full w-full items-center justify-center opacity-50", className)}>
      <p className="text-sm text-white/60 font-medium tracking-wide">{message}</p>
    </div>
  );
}