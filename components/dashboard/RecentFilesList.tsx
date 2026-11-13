"use client";

import React from "react";
import Link from "next/link";
import Thumbnail from "@/components/files/Thumbnail";
import { StorageBadge } from "@/components/files/StorageBadge";
import ActionDropdown from "@/components/files/ActionDropdown";
import { formatRelativeTime } from "@/lib/utils";

interface RecentFilesListProps {
  files: UnifiedFile[];
  onUploadClick?: () => void;
}

export const RecentFilesList = ({ files, onUploadClick }: RecentFilesListProps) => {
  return (
    <div className="flex h-full flex-col justify-between rounded-[20px] bg-white p-5 sm:p-6 shadow-sm border border-light-200/20">
      <div className="flex items-center justify-between pb-3 border-b border-light-200/20">
        <h3 className="h5 font-bold text-dark-100">Recent files uploaded</h3>
        <span className="text-xs font-semibold text-light-200">
          {files.length} {files.length === 1 ? "file" : "files"}
        </span>
      </div>

      {files.length > 0 ? (
        <ul
          className="mt-3 divide-y divide-light-200/20 overflow-y-auto max-h-[380px] remove-scrollbar pr-1"
          aria-label="Recent uploaded files"
        >
          {files.map((file) => (
            <li
              key={`${file.storageBackend}-${file.$id}`}
              className="group flex items-center justify-between gap-3 py-3 transition-colors hover:bg-light-300/50 rounded-xl px-2"
            >
              <Link
                href={file.url}
                target="_blank"
                rel="noreferrer"
                className="flex flex-1 min-w-0 items-center gap-3"
                title={file.name}
              >
                <Thumbnail
                  type={file.type}
                  extension={file.extension}
                  url={file.url}
                  unoptimized={file.storageBackend === "minio"}
                />

                <div className="flex min-w-0 flex-1 flex-col">
                  <div className="flex items-center gap-2">
                    <p
                      className="truncate text-sm font-semibold text-dark-100 group-hover:text-brand transition-colors"
                      title={file.name}
                    >
                      {file.name}
                    </p>
                    <StorageBadge backend={file.storageBackend} />
                  </div>
                  <p className="text-xs text-light-200">
                    {formatRelativeTime(file.$createdAt)}
                  </p>
                </div>
              </Link>

              <div className="shrink-0">
                <ActionDropdown file={file} />
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <div className="flex size-14 items-center justify-center rounded-full bg-light-300 mb-3">
            <svg
              className="size-6 text-light-200"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
              />
            </svg>
          </div>
          <p className="subtitle-2 font-semibold text-dark-100">No files uploaded yet</p>
          <p className="caption mt-1 text-light-200 max-w-[220px]">
            Upload documents, images or media to see them listed here.
          </p>
          {onUploadClick && (
            <button
              type="button"
              onClick={onUploadClick}
              className="mt-4 rounded-full bg-brand px-5 py-2 text-xs font-semibold text-white shadow-sm hover:bg-brand-100 transition-colors"
            >
              Upload now
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default RecentFilesList;
