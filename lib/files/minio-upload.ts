"use client";

import {
  confirmMinioUpload,
  initMinioUpload,
} from "@/lib/actions/file.actions";

export const uploadFileToMinio = async ({
  file,
  path,
}: {
  file: File;
  path: string;
}): Promise<{ success: boolean; error?: string }> => {
  const contentType = file.type || "application/octet-stream";

  const init = await initMinioUpload({
    filename: file.name,
    contentType,
    size: file.size,
  });

  if (!init) {
    return { success: false, error: "Could not prepare MinIO upload" };
  }

  const upload = await fetch(init.presignedPutUrl, {
    method: "PUT",
    body: file,
    headers: { "Content-Type": contentType },
  });

  if (!upload.ok) {
    return { success: false, error: "Upload to MinIO failed" };
  }

  const confirmed = await confirmMinioUpload({
    fileId: init.fileId,
    path,
  });

  return confirmed.success
    ? { success: true }
    : { success: false, error: confirmed.error };
};
