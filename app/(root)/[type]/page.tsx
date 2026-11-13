import React from "react";
import Sort from "@/components/search/Sort";
import { getFiles } from "@/lib/actions/file.actions";
import Card from "@/components/files/Card";
import { convertFileSize, getFileTypesParams } from "@/lib/utils";

export const dynamic = "force-dynamic";

const Page = async ({ searchParams, params }: SearchParamProps) => {
  const type = ((await params)?.type as string) || "";
  const searchText = ((await searchParams)?.query as string) || "";
  const sort = ((await searchParams)?.sort as string) || "";

  const types = getFileTypesParams(type) as FileType[];

  const files = await getFiles({ types, searchText, sort });
  const totalSize = files.documents.reduce(
    (total, file) => total + file.size,
    0,
  );

  return (
    <div className="page-container">
      <section className="w-full">
        <h1 className="h1 capitalize">{type}</h1>

        <div className="total-size-section">
          <p className="body-1">
            Total: <span className="h5">{convertFileSize(totalSize)}</span>
          </p>

          <div className="sort-container">
            <p className="body-1 hidden text-light-200 sm:block">Sort by:</p>

            <Sort />
          </div>
        </div>
      </section>

      {/* Render the files */}
      {files.documents.length > 0 ? (
        <section className="file-list">
          {files.documents.map((file: UnifiedFile) => (
            <Card
              key={`${file.storageBackend}-${file.$id}`}
              file={file}
            />
          ))}
        </section>
      ) : (
        <p className="empty-list">No files uploaded</p>
      )}
    </div>
  );
};

export default Page;
