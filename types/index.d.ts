/* eslint-disable no-unused-vars */

declare type FileType = "document" | "image" | "video" | "audio" | "other";
declare type StorageBackend = "appwrite" | "minio";

declare interface UnifiedOwner {
  fullName: string;
  email?: string;
}

declare interface UnifiedFile {
  $id: string;
  name: string;
  type: FileType;
  extension: string;
  size: number;
  url: string;
  $createdAt: string;
  $updatedAt: string;
  owner: UnifiedOwner;
  users: string[];
  bucketFileId?: string;
  storageBackend: StorageBackend;
}

declare interface GetFilesResult {
  documents: UnifiedFile[];
  total: number;
  appwrite: { documents: UnifiedFile[]; total: number };
  minio: { documents: UnifiedFile[]; total: number; available: boolean };
}

declare interface SpaceSummary {
  image: { size: number; latestDate: string };
  document: { size: number; latestDate: string };
  video: { size: number; latestDate: string };
  audio: { size: number; latestDate: string };
  other: { size: number; latestDate: string };
  used: number;
  all: number;
  available: boolean;
}

declare interface ActionType {
  label: string;
  icon: string;
  value: string;
}

declare interface SearchParamProps {
  params?: Promise<SegmentParams>;
  searchParams?: Promise<{ [key: string]: string | string[] | undefined }>;
}

declare interface UploadFileProps {
  file: File;
  ownerId: string;
  accountId: string;
  path: string;
}
declare interface GetFilesProps {
  types: FileType[];
  searchText?: string;
  sort?: string;
  limit?: number;
}
declare interface RenameFileProps {
  fileId: string;
  name: string;
  extension: string;
  path: string;
}
declare interface UpdateFileUsersProps {
  fileId: string;
  emails: string[];
  path: string;
}
declare interface DeleteFileProps {
  fileId: string;
  bucketFileId: string;
  path: string;
}

declare interface FileUploaderProps {
  ownerId: string;
  accountId: string;
  className?: string;
}

declare interface MobileNavigationProps {
  ownerId: string;
  accountId: string;
  fullName: string;
  avatar: string;
  email: string;
}
declare interface SidebarProps {
  fullName: string;
  avatar: string;
  email: string;
}

declare interface ThumbnailProps {
  type: string;
  extension: string;
  url: string;
  className?: string;
  imageClassName?: string;
  unoptimized?: boolean;
}

declare interface ShareInputProps {
  file: UnifiedFile;
  onInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onRemove: (email: string) => void;
}
