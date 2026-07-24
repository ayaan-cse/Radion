import { cn } from "@/lib/utils";

export function StatusDot({ colorClass, className }: { colorClass: string; className?: string }) {
  return (
    <span className={cn("inline-block w-2 h-2 rounded-full", colorClass, className)} />
  );
}