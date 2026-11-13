import React from "react";

export const DashboardSkeleton = () => {
  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 md:gap-8 pb-10 animate-pulse">
      {/* Overview Cards Skeleton */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
        {[1, 2].map((i) => (
          <div
            key={i}
            className="flex min-h-[160px] max-h-[190px] w-full items-center justify-between rounded-[20px] bg-white p-6 shadow-sm border border-light-200/20"
          >
            <div className="space-y-3 flex-1">
              <div className="h-4 w-24 bg-light-300 rounded" />
              <div className="h-8 w-36 bg-light-300 rounded" />
              <div className="h-3 w-48 bg-light-300 rounded" />
            </div>
            <div className="size-20 rounded-full bg-light-300" />
          </div>
        ))}
      </div>

      {/* Grid Skeleton */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-12 md:gap-8">
        <div className="lg:col-span-7 space-y-4">
          <div className="flex justify-between items-center">
            <div className="h-5 w-40 bg-light-300 rounded" />
            <div className="h-8 w-36 bg-light-300 rounded" />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="h-[175px] rounded-[20px] bg-white p-5 border border-light-200/20 flex flex-col justify-between"
              >
                <div className="flex justify-between items-start">
                  <div className="size-14 rounded-full bg-light-300" />
                  <div className="h-6 w-20 bg-light-300 rounded" />
                </div>
                <div className="pt-3 border-t border-light-200/20 space-y-1.5">
                  <div className="h-4 w-24 bg-light-300 rounded" />
                  <div className="h-3 w-36 bg-light-300 rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="lg:col-span-5">
          <div className="h-[380px] rounded-[20px] bg-white p-6 border border-light-200/20 space-y-4">
            <div className="h-5 w-36 bg-light-300 rounded" />
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="flex items-center gap-3 py-2">
                <div className="size-10 rounded-full bg-light-300" />
                <div className="space-y-2 flex-1">
                  <div className="h-4 w-32 bg-light-300 rounded" />
                  <div className="h-3 w-20 bg-light-300 rounded" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardSkeleton;
