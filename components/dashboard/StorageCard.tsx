"use client";

import React from "react";
import { Button } from "@/components/ui/button";
import { convertFileSize, calculatePercentage, formatPercentage } from "@/lib/utils";
import { useRouter } from "next/navigation";

interface StorageCardProps {
  label: "Appwrite" | "MinIO";
  used?: number;
  total?: number;
  available: boolean;
  color?: string;
}

export const StorageCard = ({
  label,
  used = 0,
  total = 2 * 1024 * 1024 * 1024,
  available,
  color = "#FA7275",
}: StorageCardProps) => {
  const router = useRouter();
  const percentage = calculatePercentage(used, total);
  const formattedPercent = formatPercentage(percentage);

  // SVG circular progress ring calculations
  const size = 100;
  const strokeWidth = 8;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  // Limit max stroke dashoffset to 100%
  const offset = available
    ? circumference - (Math.min(percentage, 100) / 100) * circumference
    : circumference;

  return (
    <div
      className={`relative flex min-h-[160px] max-h-[190px] w-full items-center justify-between rounded-[20px] bg-white p-5 sm:p-6 shadow-sm border transition-all duration-200 ${
        available
          ? "border-light-200/20 hover:shadow-md"
          : "border-red/20 bg-red/5"
      }`}
      aria-label={`${label} Storage Overview`}
    >
      <div className="flex flex-1 flex-col justify-between h-full gap-2">
        {/* Header: Name + Status badge */}
        <div className="flex items-center gap-2.5">
          <h3 className="h4 text-dark-100 font-bold tracking-tight">{label}</h3>
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
              available
                ? "bg-emerald-50 text-emerald-600 border border-emerald-200"
                : "bg-red/10 text-red border border-red/20"
            }`}
          >
            <span
              className={`size-1.5 rounded-full ${
                available ? "bg-emerald-500 animate-pulse" : "bg-red"
              }`}
            />
            {available ? "Online" : "Unavailable"}
          </span>
        </div>

        {/* Numbers or Unavailable State */}
        {available ? (
          <div className="space-y-1">
            <p className="text-2xl sm:text-3xl font-extrabold text-dark-200">
              {convertFileSize(used)}
            </p>
            <p className="text-xs sm:text-sm font-medium text-light-100/70">
              of {convertFileSize(total)} allocated
            </p>
          </div>
        ) : (
          <div className="space-y-2">
            <p className="text-xs sm:text-sm font-medium text-red">
              Backend is currently unreachable.
            </p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => router.refresh()}
              className="h-8 rounded-lg border-red/30 text-xs font-semibold text-red hover:bg-red/10"
              aria-label={`Retry connecting to ${label}`}
            >
              Retry Connection
            </Button>
          </div>
        )}

        <div className="text-xs text-light-200">
          {available ? "2.0 GB Free Quota" : "Service offline"}
        </div>
      </div>

      {/* Circular Progress Ring */}
      <div className="relative flex shrink-0 items-center justify-center pl-2">
        <svg
          width={size}
          height={size}
          className="rotate-[-90deg] transition-all"
          aria-hidden="true"
        >
          {/* Background Track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="transparent"
            stroke={available ? "#F2F4F8" : "#FEE2E2"}
            strokeWidth={strokeWidth}
          />
          {/* Active Progress */}
          {available && (
            <circle
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="transparent"
              stroke={color}
              strokeWidth={strokeWidth}
              strokeDasharray={circumference}
              strokeDashoffset={offset}
              strokeLinecap="round"
              className="transition-[stroke-dashoffset] duration-1000 ease-out"
            />
          )}
        </svg>

        {/* Center Percentage Display */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
          {available ? (
            <span className="text-lg font-bold text-dark-100">
              {formattedPercent}
            </span>
          ) : (
            <span className="text-xs font-bold text-red">Offline</span>
          )}
          <span className="text-[10px] font-medium text-light-200 uppercase tracking-wider">
            Used
          </span>
        </div>
      </div>
    </div>
  );
};

export default StorageCard;
