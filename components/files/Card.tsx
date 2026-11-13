import Link from "next/link";
import Thumbnail from "@/components/files/Thumbnail";
import { convertFileSize } from "@/lib/utils";
import FormattedDateTime from "@/components/common/FormattedDateTime";
import ActionDropdown from "@/components/files/ActionDropdown";
import { StorageBadge } from "@/components/files/StorageBadge";

const Card = ({ file }: { file: UnifiedFile }) => {
  return (
    <Link href={file.url} target="_blank" className="file-card">
      <div className="flex justify-between">
        <Thumbnail
          type={file.type}
          extension={file.extension}
          url={file.url}
          className="!size-20"
          imageClassName="!size-11"
          unoptimized={file.storageBackend === "minio"}
        />

        <div className="flex flex-col items-end justify-between">
          <ActionDropdown file={file} />
          <div className="flex flex-col items-end gap-1">
            <StorageBadge backend={file.storageBackend} />
            <p className="body-1">{convertFileSize(file.size)}</p>
          </div>
        </div>
      </div>

      <div className="file-card-details">
        <p className="subtitle-2 line-clamp-1">{file.name}</p>
        <FormattedDateTime
          date={file.$createdAt}
          className="body-2 text-light-100"
        />
        <p className="caption line-clamp-1 text-light-200">
          By: {file.owner.fullName}
        </p>
      </div>
    </Link>
  );
};
export default Card;
