import { cn } from "@/lib/utils";
import { storageBackendLabel } from "@/lib/files/unified";

const styles: Record<StorageBackend, string> = {
  appwrite:
    "border-sky-400/30 bg-sky-500/15 text-sky-300",
  minio:
    "border-emerald-400/30 bg-emerald-500/15 text-emerald-300",
};

export const StorageBadge = ({
  backend,
  className,
}: {
  backend: StorageBackend;
  className?: string;
}) => (
  <span
    className={cn(
      "inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
      styles[backend],
      className,
    )}
  >
    {storageBackendLabel(backend)}
  </span>
);
