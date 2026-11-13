import { apiFetch } from "@/lib/api/client";

export interface BackendFileDto {
  id: string;
  ownerId: string;
  filename: string;
  objectKey: string;
  size: number;
  contentType: string;
  extension?: string;
  fileType: string;
  status: "PENDING" | "ACTIVE" | "FAILED";
  sharedWith?: string[];
  createdAt: string;
  updatedAt: string;
  url?: string;
  ownerName?: string;
  ownerEmail?: string;
  storageBackend: "minio";
}

export interface BackendPage<T> {
  content: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
  size?: number;
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface MinioUploadInit {
  fileId: string;
  presignedPutUrl: string;
}

export interface MinioSpace {
  usedBytes: number;
  totalBytes: number;
}

export interface MinioListParams {
  types?: FileType[];
  searchText?: string;
  sort?: string;
  limit?: number;
}

const splitSort = (sort?: string) => {
  const [field, order] = (sort || "$createdAt-desc").split("-");
  const sortField =
    field === "name" ? "name" : field === "size" ? "size" : "date";

  return { sortField, sortDir: order === "asc" ? "asc" : "desc" };
};

const buildListQuery = ({
  types = [],
  searchText,
  sort,
  limit,
}: MinioListParams) => {
  const params = new URLSearchParams();
  const { sortField, sortDir } = splitSort(sort);

  if (types.length > 0) params.set("type", types.join(","));
  if (searchText) params.set("search", searchText);
  params.set("sort", sortField);
  params.set("order", sortDir);
  params.set("size", String(limit && limit > 0 ? limit : 100));

  return params.toString();
};

export const listMinioFilesRequest = (
  accessToken: string,
  params: MinioListParams,
) =>
  apiFetch<BackendPage<BackendFileDto>>(
    `/api/files?${buildListQuery(params)}`,
    { accessToken, cache: "no-store" },
  );

export const initMinioUploadRequest = (
  accessToken: string,
  input: { filename: string; contentType: string; size: number },
) =>
  apiFetch<MinioUploadInit>("/api/files/upload-url", {
    method: "POST",
    accessToken,
    body: input,
  });

export const confirmMinioUploadRequest = (
  accessToken: string,
  fileId: string,
) =>
  apiFetch<BackendFileDto>(`/api/files/${fileId}/confirm`, {
    method: "POST",
    accessToken,
  });

export const renameMinioFileRequest = (
  accessToken: string,
  fileId: string,
  name: string,
) =>
  apiFetch<BackendFileDto>(`/api/files/${fileId}/rename`, {
    method: "PATCH",
    accessToken,
    body: { name },
  });

export const deleteMinioFileRequest = (
  accessToken: string,
  fileId: string,
) =>
  apiFetch<void>(`/api/files/${fileId}`, {
    method: "DELETE",
    accessToken,
  });

export const shareMinioFileRequest = (
  accessToken: string,
  fileId: string,
  emails: string[],
) =>
  apiFetch<BackendFileDto>(`/api/files/${fileId}/share`, {
    method: "PATCH",
    accessToken,
    body: { emails },
  });

export const downloadMinioFileRequest = (
  accessToken: string,
  fileId: string,
) =>
  apiFetch<{ presignedGetUrl: string }>(
    `/api/files/${fileId}/download-url`,
    { accessToken, cache: "no-store" },
  );

export const minioSpaceRequest = (accessToken: string) =>
  apiFetch<MinioSpace>("/api/files/space", {
    accessToken,
    cache: "no-store",
  });
