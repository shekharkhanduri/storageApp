import React from "react";
import Image from "next/image";
import Link from "next/link";
import { convertFileSize, formatDateTime } from "@/lib/utils";

interface CategoryTileProps {
  title: string;
  size: number;
  count?: number;
  latestDate?: string;
  icon: string;
  url: string;
}

export const CategoryTile = ({
  title,
  size,
  count,
  latestDate,
  icon,
  url,
}: CategoryTileProps) => {
  const hasFiles = (count !== undefined && count > 0) || size > 0;

  return (
    <Link
      href={url}
      className="group relative flex flex-col justify-between overflow-hidden rounded-[20px] bg-white p-5 sm:p-6 min-h-[175px] shadow-sm border border-light-200/20 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:border-brand/30 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand"
      aria-label={`View ${title} files`}
    >
      {/* Top row: Signature illustration icon on left, file size on right */}
      <div className="relative flex items-start justify-between">
        <Image
          src={icon}
          width={190}
          height={95}
          alt={`${title} banner`}
          className="absolute -left-5 -top-6 z-0 w-[170px] sm:w-[185px] object-contain pointer-events-none select-none transition-transform duration-300 group-hover:scale-105"
        />

        <div className="relative z-10 w-full text-right">
          <h4 className="text-lg sm:text-2xl font-bold text-dark-100">
            {hasFiles ? convertFileSize(size) : "0 Bytes"}
          </h4>
          {typeof count === "number" && (
            <p className="text-xs font-medium text-light-100/70 mt-0.5">
              {count} {count === 1 ? "file" : "files"}
            </p>
          )}
        </div>
      </div>

      {/* Bottom row: Category title, separator, and last updated time / friendly empty state */}
      <div className="relative z-10 mt-6 pt-3 border-t border-light-200/20 flex flex-col gap-0.5">
        <h5 className="subtitle-2 font-semibold text-dark-100 group-hover:text-brand transition-colors">
          {title}
        </h5>
        <p className="text-xs text-light-200">
          {hasFiles && latestDate ? (
            `Updated ${formatDateTime(latestDate)}`
          ) : (
            <span className="italic text-light-200/80">No {title.toLowerCase()} yet</span>
          )}
        </p>
      </div>
    </Link>
  );
};

export default CategoryTile;
