"use server";

import { createAdminClient } from "@/lib/appwrite";
import { InputFile } from "node-appwrite/file";
import { appwriteConfig } from "@/lib/appwrite/config";
import { ID, Query } from "node-appwrite";
import type { Models } from "node-appwrite";
import { constructFileUrl, getFileType, parseStringify } from "@/lib/utils";
import { revalidatePath } from "next/cache";
import { getCurrentUser } from "@/lib/actions/user.actions";
import { getAccessToken, refreshAccessToken } from "@/lib/auth/session";
import {
  confirmMinioUploadRequest,
  deleteMinioFileRequest,
  downloadMinioFileRequest,
  initMinioUploadRequest,
  listMinioFilesRequest,
  minioSpaceRequest,
  renameMinioFileRequest,
  shareMinioFileRequest,
} from "@/lib/api/file.api";
import {
  emptySpaceSummary,
  mergeUnifiedFiles,
  summarizeFiles,
  toUnifiedAppwrite,
  toUnifiedMinio,
} from "@/lib/files/unified";

const handleError = (error: unknown, message: string) => {
  console.log(error, message);
  throw error;
};

type ApiResult<T> = { res: Response; data: T | null };

const withMinioAuth = async <T>(
  request: (accessToken: string) => Promise<ApiResult<T>>,
): Promise<ApiResult<T> | null> => {
  let token = await getAccessToken();

  if (!token) token = await refreshAccessToken();
  if (!token) return null;

  let result = await request(token);

  if (result.res.status === 401) {
    const refreshed = await refreshAccessToken();
    if (!refreshed) return null;
    result = await request(refreshed);
  }

  return result;
};

export const uploadFile = async ({
  file,
  ownerId,
  accountId,
  path,
}: UploadFileProps) => {
  const { storage, databases } = await createAdminClient();

  try {
    const inputFile = InputFile.fromBuffer(file, file.name);

    const bucketFile = await storage.createFile(
      appwriteConfig.bucketId,
      ID.unique(),
      inputFile,
    );

    const fileDocument = {
      type: getFileType(bucketFile.name).type,
      name: bucketFile.name,
      url: constructFileUrl(bucketFile.$id),
      extension: getFileType(bucketFile.name).extension,
      size: bucketFile.sizeOriginal,
      owner: ownerId,
      accountId,
      users: [],
      bucketFileId: bucketFile.$id,
    };

    const newFile = await databases
      .createDocument(
        appwriteConfig.databaseId,
        appwriteConfig.filesCollectionId,
        ID.unique(),
        fileDocument,
      )
      .catch(async (error: unknown) => {
        await storage.deleteFile(appwriteConfig.bucketId, bucketFile.$id);
        handleError(error, "Failed to create file document");
      });

    revalidatePath(path);
    return parseStringify(newFile);
  } catch (error) {
    handleError(error, "Failed to upload file");
  }
};

const createQueries = (
  currentUser: Models.Document,
  types: string[],
  searchText: string,
  sort: string,
  limit?: number,
) => {
  const queries = [
    Query.or([
      Query.equal("owner", [currentUser.$id]),
      Query.contains("users", [currentUser.email]),
    ]),
  ];

  if (types.length > 0) queries.push(Query.equal("type", types));
  if (searchText) queries.push(Query.contains("name", searchText));
  if (limit) queries.push(Query.limit(limit));

  if (sort) {
    const [sortBy, orderBy] = sort.split("-");

    queries.push(
      orderBy === "asc" ? Query.orderAsc(sortBy) : Query.orderDesc(sortBy),
    );
  }

  return queries;
};

export const getAppwriteFiles = async ({
  types = [],
  searchText = "",
  sort = "$createdAt-desc",
  limit,
}: GetFilesProps): Promise<{ documents: UnifiedFile[]; total: number }> => {
  const { databases } = await createAdminClient();
  const currentUser = await getCurrentUser();

  if (!currentUser) return { documents: [], total: 0 };

  const queries = createQueries(currentUser, types, searchText, sort, limit);

  const files = await databases.listDocuments(
    appwriteConfig.databaseId,
    appwriteConfig.filesCollectionId,
    queries,
  );

  return {
    documents: files.documents.map(toUnifiedAppwrite),
    total: files.total,
  };
};

export const getMinioFiles = async ({
  types = [],
  searchText = "",
  sort = "$createdAt-desc",
  limit,
}: GetFilesProps): Promise<{
  documents: UnifiedFile[];
  total: number;
  available: boolean;
}> => {
  try {
    const result = await withMinioAuth((token) =>
      listMinioFilesRequest(token, { types, searchText, sort, limit }),
    );

    if (!result || !result.res.ok || !result.data) {
      return { documents: [], total: 0, available: false };
    }

    const total =
      result.data.page?.totalElements ??
      result.data.totalElements ??
      result.data.content?.length ??
      0;

    return {
      documents: result.data.content.map(toUnifiedMinio),
      total,
      available: true,
    };
  } catch {
    return { documents: [], total: 0, available: false };
  }
};

export const getFiles = async ({
  types = [],
  searchText = "",
  sort = "$createdAt-desc",
  limit,
}: GetFilesProps): Promise<GetFilesResult> => {
  const requestLimit = limit ?? 100;

  const [appwriteResult, minioResult] = await Promise.all([
    getAppwriteFiles({ types, searchText, sort, limit: requestLimit }).catch(
      () => ({ documents: [], total: 0 }),
    ),
    getMinioFiles({ types, searchText, sort, limit: requestLimit }),
  ]);

  const merged = mergeUnifiedFiles(
    appwriteResult.documents,
    minioResult.documents,
    sort,
  );

  const appwriteTotal = Number(appwriteResult.total) || 0;
  const minioTotal = Number(minioResult.total) || 0;
  const combinedTotal = appwriteTotal + minioTotal;

  return {
    documents: limit ? merged.slice(0, limit) : merged,
    total: isNaN(combinedTotal) ? merged.length : combinedTotal,
    appwrite: appwriteResult,
    minio: {
      documents: minioResult.documents,
      total: minioTotal,
      available: minioResult.available,
    },
  };
};

export const renameFile = async ({
  fileId,
  name,
  extension,
  path,
}: RenameFileProps) => {
  const { databases } = await createAdminClient();

  try {
    const newName = extension ? `${name}.${extension}` : name;
    const updatedFile = await databases.updateDocument(
      appwriteConfig.databaseId,
      appwriteConfig.filesCollectionId,
      fileId,
      {
        name: newName,
      },
    );

    revalidatePath(path);
    return parseStringify(updatedFile);
  } catch (error) {
    handleError(error, "Failed to rename file");
  }
};

export const updateFileUsers = async ({
  fileId,
  emails,
  path,
}: UpdateFileUsersProps) => {
  const { databases } = await createAdminClient();

  try {
    const updatedFile = await databases.updateDocument(
      appwriteConfig.databaseId,
      appwriteConfig.filesCollectionId,
      fileId,
      {
        users: emails,
      },
    );

    revalidatePath(path);
    return parseStringify(updatedFile);
  } catch (error) {
    handleError(error, "Failed to share file");
  }
};

export const deleteFile = async ({
  fileId,
  bucketFileId,
  path,
}: DeleteFileProps) => {
  const { databases, storage } = await createAdminClient();

  try {
    const deletedFile = await databases.deleteDocument(
      appwriteConfig.databaseId,
      appwriteConfig.filesCollectionId,
      fileId,
    );

    if (deletedFile) {
      await storage.deleteFile(appwriteConfig.bucketId, bucketFileId);
    }

    revalidatePath(path);
    return parseStringify({ status: "success" });
  } catch (error) {
    handleError(error, "Failed to delete file");
  }
};

// ============================== MINIO FILE ACTIONS
export const initMinioUpload = async (input: {
  filename: string;
  contentType: string;
  size: number;
}) => {
  const result = await withMinioAuth((token) =>
    initMinioUploadRequest(token, input),
  );

  if (!result || !result.res.ok || !result.data) return null;

  return result.data;
};

export const confirmMinioUpload = async ({
  fileId,
  path,
}: {
  fileId: string;
  path: string;
}) => {
  const result = await withMinioAuth((token) =>
    confirmMinioUploadRequest(token, fileId),
  );

  if (!result || !result.res.ok || !result.data) {
    return { success: false, error: "Failed to confirm MinIO upload" };
  }

  revalidatePath(path);
  return { success: true, file: toUnifiedMinio(result.data) };
};

export const renameMinioFile = async ({
  fileId,
  name,
  extension,
  path,
}: RenameFileProps) => {
  const newName = extension ? `${name}.${extension}` : name;

  const result = await withMinioAuth((token) =>
    renameMinioFileRequest(token, fileId, newName),
  );

  if (!result || !result.res.ok) {
    return { success: false, error: "Failed to rename MinIO file" };
  }

  revalidatePath(path);
  return { success: true };
};

export const shareMinioFile = async ({
  fileId,
  emails,
  path,
}: UpdateFileUsersProps) => {
  const result = await withMinioAuth((token) =>
    shareMinioFileRequest(token, fileId, emails),
  );

  if (!result || !result.res.ok) {
    return { success: false, error: "Failed to share MinIO file" };
  }

  revalidatePath(path);
  return { success: true };
};

export const deleteMinioFile = async ({
  fileId,
  path,
}: {
  fileId: string;
  path: string;
}) => {
  const result = await withMinioAuth((token) =>
    deleteMinioFileRequest(token, fileId),
  );

  if (!result || !result.res.ok) {
    return { success: false, error: "Failed to delete MinIO file" };
  }

  revalidatePath(path);
  return { success: true };
};

export const getMinioDownloadUrl = async (fileId: string) => {
  const result = await withMinioAuth((token) =>
    downloadMinioFileRequest(token, fileId),
  );

  if (!result || !result.res.ok || !result.data) return null;

  return result.data.presignedGetUrl;
};

// ============================== TOTAL FILE SPACE USED
const getAppwriteSpaceUsed = async (): Promise<SpaceSummary> => {
  const { databases } = await createAdminClient();
  const currentUser = await getCurrentUser();

  if (!currentUser) return emptySpaceSummary();

  const files = await databases.listDocuments(
    appwriteConfig.databaseId,
    appwriteConfig.filesCollectionId,
    [Query.equal("owner", [currentUser.$id]), Query.limit(1000)],
  );

  return summarizeFiles(files.documents.map(toUnifiedAppwrite));
};

const getMinioSpaceUsed = async (): Promise<SpaceSummary> => {
  const [list, space] = await Promise.all([
    getMinioFiles({ types: [], limit: 1000 }),
    withMinioAuth((token) => minioSpaceRequest(token)),
  ]);

  const totalBytes =
    space && space.res.ok && space.data
      ? space.data.totalBytes
      : 2 * 1024 * 1024 * 1024;

  const summary = summarizeFiles(
    list.documents,
    totalBytes,
    list.available,
  );

  if (space && space.res.ok && space.data) {
    summary.used = space.data.usedBytes;
  }

  return summary;
};

export async function getTotalSpaceUsed(): Promise<{
  appwrite: SpaceSummary;
  minio: SpaceSummary;
}> {
  const [appwrite, minio] = await Promise.all([
    getAppwriteSpaceUsed().catch(() => ({
      ...emptySpaceSummary(),
      available: false,
    })),
    getMinioSpaceUsed().catch(() => ({
      ...emptySpaceSummary(),
      available: false,
    })),
  ]);

  return { appwrite, minio };
}
