"use client";

import React, { useState } from "react";
import StorageCard from "@/components/dashboard/StorageCard";
import BackendSwitcher from "@/components/dashboard/BackendSwitcher";
import CategoryTile from "@/components/dashboard/CategoryTile";
import RecentFilesList from "@/components/dashboard/RecentFilesList";

interface DashboardContentProps {
  totalSpace: {
    appwrite: SpaceSummary;
    minio: SpaceSummary;
  };
  files: UnifiedFile[];
  appwriteFiles: UnifiedFile[];
  minioFiles: UnifiedFile[];
}

export const DashboardContent = ({
  totalSpace,
  files,
  appwriteFiles,
  minioFiles,
}: DashboardContentProps) => {
  const [selectedBackend, setSelectedBackend] = useState<StorageBackend>("appwrite");

  // Get active summary based on selected backend
  const activeSummary =
    selectedBackend === "appwrite" ? totalSpace.appwrite : totalSpace.minio;
  const activeFiles = selectedBackend === "appwrite" ? appwriteFiles : minioFiles;

  // File counts per category for the selected backend
  const getCategoryCount = (type: string) => {
    if (type === "media") {
      return activeFiles.filter((f) => f.type === "video" || f.type === "audio").length;
    }
    return activeFiles.filter((f) => f.type === type).length;
  };

  const categories = [
    {
      title: "Documents",
      size: activeSummary.document?.size || 0,
      count: getCategoryCount("document"),
      latestDate: activeSummary.document?.latestDate,
      icon: "/assets/icons/file-document-light.svg",
      url: "/documents",
    },
    {
      title: "Images",
      size: activeSummary.image?.size || 0,
      count: getCategoryCount("image"),
      latestDate: activeSummary.image?.latestDate,
      icon: "/assets/icons/file-image-light.svg",
      url: "/images",
    },
    {
      title: "Media",
      size: (activeSummary.video?.size || 0) + (activeSummary.audio?.size || 0),
      count: getCategoryCount("media"),
      latestDate:
        (activeSummary.video?.latestDate || "") > (activeSummary.audio?.latestDate || "")
          ? activeSummary.video?.latestDate
          : activeSummary.audio?.latestDate,
      icon: "/assets/icons/file-video-light.svg",
      url: "/media",
    },
    {
      title: "Others",
      size: activeSummary.other?.size || 0,
      count: getCategoryCount("other"),
      latestDate: activeSummary.other?.latestDate,
      icon: "/assets/icons/file-other-light.svg",
      url: "/others",
    },
  ];

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 md:gap-8 pb-10">
      {/* 1. STORAGE OVERVIEW ROW (Top of page, full width, side by side) */}
      <section aria-label="Storage Providers Overview">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:gap-6">
          <StorageCard
            label="Appwrite"
            used={totalSpace.appwrite.used}
            total={totalSpace.appwrite.all}
            available={totalSpace.appwrite.available}
            color="#FA7275" // Brand Coral
          />
          <StorageCard
            label="MinIO"
            used={totalSpace.minio.used}
            total={totalSpace.minio.all}
            available={totalSpace.minio.available}
            color="#2dd4bf" // Teal
          />
        </div>
      </section>

      {/* 2. BREAKDOWN + RECENT FILES (Two-column layout on desktop) */}
      <section
        aria-label="Storage Breakdown and Recent Uploads"
        className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12 md:gap-8"
      >
        {/* Left Column: Category Breakdown (~60%) */}
        <div className="flex flex-col gap-4 lg:col-span-7">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="h5 font-bold text-dark-100">Category breakdown</h3>
              <p className="caption text-light-200">
                Storage allocated across file types for {selectedBackend === "appwrite" ? "Appwrite" : "MinIO"}
              </p>
            </div>

            {/* Segmented Backend Switcher */}
            <BackendSwitcher
              activeBackend={selectedBackend}
              onSelect={setSelectedBackend}
              appwriteCount={appwriteFiles.length}
              minioCount={minioFiles.length}
            />
          </div>

          {/* 2x2 Equal-size Grid */}
          <div
            id={`${selectedBackend}-breakdown`}
            role="tabpanel"
            aria-label={`${selectedBackend} breakdown`}
            className="grid grid-cols-1 gap-4 sm:grid-cols-2"
          >
            {categories.map((cat) => (
              <CategoryTile
                key={cat.title}
                title={cat.title}
                size={cat.size}
                count={cat.count}
                latestDate={cat.latestDate}
                icon={cat.icon}
                url={cat.url}
              />
            ))}
          </div>
        </div>

        {/* Right Column: Recent Files Uploaded (~40%) */}
        <div className="lg:col-span-5">
          <RecentFilesList
            files={files}
            onUploadClick={() => {
              const uploaderBtn = document.querySelector(".uploader-button") as HTMLButtonElement | null;
              if (uploaderBtn) uploaderBtn.click();
            }}
          />
        </div>
      </section>
    </div>
  );
};

export default DashboardContent;
