import React from "react";
import Image from "next/image";
import { cn, getFileIcon } from "@/lib/utils";

interface Props {
  type: string;
  extension: string;
  url?: string;
  imageClassName?: string;
  className?: string;
  unoptimized?: boolean;
}

export const Thumbnail = ({
  type,
  extension,
  url = "",
  imageClassName,
  className,
  unoptimized = false,
}: Props) => {
  const isImage =
    type === "image" &&
    extension !== "svg" &&
    extension !== "heic" &&
    extension !== "heif";

  return (
    <figure className={cn("thumbnail", className)}>
      <Image
        src={isImage ? url : getFileIcon(extension, type)}
        alt="thumbnail"
        width={100}
        height={100}
        unoptimized={unoptimized}
        className={cn(
          "size-8 object-contain",
          imageClassName,
          isImage && "thumbnail-image",
        )}
      />
    </figure>
  );
};
export default Thumbnail;
