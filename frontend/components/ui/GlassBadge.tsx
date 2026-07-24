import { cn } from "@/lib/utils";

export function GlassBadge({
  children,
  colorClass,
  className,
}: {
  children: React.ReactNode;
  colorClass: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "px-2.5 py-1 rounded-md text-xs font-medium bg-white/5 border border-white/10 backdrop-blur-md",
        colorClass,
        className
      )}
    >
      {children}
    </span>
  );
}