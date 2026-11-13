import type { Models } from "node-appwrite";

import type { BackendFileDto } from "@/lib/api/file.api";

const TWO_GB = 2 * 1024 * 1024 * 1024;

const asFileType = (value: unknown): FileType => {
  if (
    value === "document" ||
    value === "image" ||
    value === "video" ||
    value === "audio"
  ) {
    return value;
  }

  return "other";
};

const fallbackExtension = (filename: string) => {
  const dot = filename.lastIndexOf(".");
  return dot > -1 ? filename.slice(dot + 1).toLowerCase() : "";
};

export const toUnifiedAppwrite = (file: Models.Document): UnifiedFile => {
  const owner = (file.owner ?? {}) as { fullName?: string; email?: string };

  return {
    $id: file.$id,
    name: file.name,
    type: asFileType(file.type),
    extension: file.extension || fallbackExtension(file.name),
    size: file.size ?? 0,
    url: file.url || "",
    $createdAt: file.$createdAt,
    $updatedAt: file.$updatedAt,
    owner: {
      fullName: owner.fullName || "Unknown",
      email: owner.email,
    },
    users: Array.isArray(file.users) ? file.users : [],
    bucketFileId: file.bucketFileId,
    storageBackend: "appwrite",
  };
};

export const toUnifiedMinio = (file: BackendFileDto): UnifiedFile => ({
  $id: file.id,
  name: file.filename,
  type: asFileType(file.fileType),
  extension: file.extension || fallbackExtension(file.filename),
  size: file.size ?? 0,
  url: file.url || "",
  $createdAt: file.createdAt,
  $updatedAt: file.updatedAt,
  owner: {
    fullName: file.ownerName || "Unknown",
    email: file.ownerEmail,
  },
  users: file.sharedWith ?? [],
  storageBackend: "minio",
});

export const sortUnifiedFiles = (files: UnifiedFile[], sort?: string) => {
  const [field, order] = (sort || "$createdAt-desc").split("-");
  const direction = order === "asc" ? 1 : -1;

  return [...files].sort((a, b) => {
    if (field === "name") {
      return a.name.localeCompare(b.name) * direction;
    }

    if (field === "size") {
      return (a.size - b.size) * direction;
    }

    return (
      (new Date(a.$createdAt).getTime() - new Date(b.$createdAt).getTime()) *
      direction
    );
  });
};

export const mergeUnifiedFiles = (
  appwrite: UnifiedFile[],
  minio: UnifiedFile[],
  sort?: string,
) => sortUnifiedFiles([...appwrite, ...minio], sort);

export const emptySpaceSummary = (totalBytes = TWO_GB): SpaceSummary => ({
  image: { size: 0, latestDate: "" },
  document: { size: 0, latestDate: "" },
  video: { size: 0, latestDate: "" },
  audio: { size: 0, latestDate: "" },
  other: { size: 0, latestDate: "" },
  used: 0,
  all: totalBytes,
  available: true,
});

export const summarizeFiles = (
  files: UnifiedFile[],
  totalBytes = TWO_GB,
  available = true,
): SpaceSummary => {
  const summary = emptySpaceSummary(totalBytes);
  summary.available = available;

  for (const file of files) {
    const bucket = summary[file.type];
    bucket.size += file.size;

    if (!bucket.latestDate || file.$updatedAt > bucket.latestDate) {
      bucket.latestDate = file.$updatedAt;
    }
  }

  summary.used = files.reduce((total, file) => total + file.size, 0);

  return summary;
};

export const storageBackendLabel = (backend: StorageBackend) =>
  backend === "minio" ? "MinIO" : "Appwrite";
