"use client";

import React from "react";
import { cn } from "@/lib/utils";

interface BackendSwitcherProps {
  activeBackend: StorageBackend;
  onSelect: (backend: StorageBackend) => void;
  appwriteCount?: number;
  minioCount?: number;
}

export const BackendSwitcher = ({
  activeBackend,
  onSelect,
  appwriteCount,
  minioCount,
}: BackendSwitcherProps) => {
  return (
    <div
      role="tablist"
      aria-label="Filter storage category breakdown by backend"
      className="inline-flex items-center rounded-xl bg-light-300 p-1 border border-light-200/30"
    >
      <button
        type="button"
        role="tab"
        aria-selected={activeBackend === "appwrite"}
        aria-controls="appwrite-breakdown"
        onClick={() => onSelect("appwrite")}
        className={cn(
          "flex items-center gap-2 rounded-lg px-4 py-1.5 text-xs font-semibold tracking-wide transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand",
          activeBackend === "appwrite"
            ? "bg-white text-dark-100 shadow-sm"
            : "text-light-100 hover:text-dark-100",
        )}
      >
        <span>Appwrite</span>
        {typeof appwriteCount === "number" && (
          <span
            className={cn(
              "rounded-full px-1.5 py-0.2 text-[10px] font-bold",
              activeBackend === "appwrite"
                ? "bg-brand/10 text-brand"
                : "bg-light-200/30 text-light-100",
            )}
          >
            {appwriteCount}
          </span>
        )}
      </button>

      <button
        type="button"
        role="tab"
        aria-selected={activeBackend === "minio"}
        aria-controls="minio-breakdown"
        onClick={() => onSelect("minio")}
        className={cn(
          "flex items-center gap-2 rounded-lg px-4 py-1.5 text-xs font-semibold tracking-wide transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand",
          activeBackend === "minio"
            ? "bg-white text-dark-100 shadow-sm"
            : "text-light-100 hover:text-dark-100",
        )}
      >
        <span>MinIO</span>
        {typeof minioCount === "number" && (
          <span
            className={cn(
              "rounded-full px-1.5 py-0.2 text-[10px] font-bold",
              activeBackend === "minio"
                ? "bg-emerald-100 text-emerald-700"
                : "bg-light-200/30 text-light-100",
            )}
          >
            {minioCount}
          </span>
        )}
      </button>
    </div>
  );
};

export default BackendSwitcher;
