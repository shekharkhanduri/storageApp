import { describe, expect, it } from "vitest";

import type { BackendFileDto } from "@/lib/api/file.api";
import {
  emptySpaceSummary,
  mergeUnifiedFiles,
  sortUnifiedFiles,
  storageBackendLabel,
  summarizeFiles,
  toUnifiedAppwrite,
  toUnifiedMinio,
} from "@/lib/files/unified";
import { calculatePercentage } from "@/lib/utils";

const appwriteDoc = {
  $id: "aw-1",
  $createdAt: "2024-01-01T10:00:00.000Z",
  $updatedAt: "2024-01-02T10:00:00.000Z",
  name: "notes.pdf",
  type: "document",
  extension: "pdf",
  size: 120,
  url: "https://cloud.appwrite.io/file/aw-1",
  owner: { fullName: "Ada", email: "ada@example.com" },
  users: ["friend@example.com"],
  bucketFileId: "bucket-1",
};

const minioDto: BackendFileDto = {
  id: "minio-1",
  ownerId: "owner-1",
  filename: "clip.mp4",
  objectKey: "owner-1/uuid-clip.mp4",
  size: 240,
  contentType: "video/mp4",
  extension: "mp4",
  fileType: "video",
  status: "ACTIVE",
  sharedWith: ["friend@example.com"],
  createdAt: "2024-03-01T10:00:00.000Z",
  updatedAt: "2024-03-02T10:00:00.000Z",
  url: "http://localhost:9000/storeit-files/owner-1/uuid-clip.mp4",
  ownerName: "Grace",
  ownerEmail: "grace@example.com",
  storageBackend: "minio",
};

const makeFile = (overrides: Partial<UnifiedFile>): UnifiedFile => ({
  $id: "file",
  name: "file",
  type: "document",
  extension: "txt",
  size: 0,
  url: "",
  $createdAt: "2024-01-01T00:00:00.000Z",
  $updatedAt: "2024-01-01T00:00:00.000Z",
  owner: { fullName: "Owner" },
  users: [],
  storageBackend: "appwrite",
  ...overrides,
});

describe("unified file mapping", () => {
  it("maps an Appwrite document without touching its backend", () => {
    const file = toUnifiedAppwrite(appwriteDoc as never);

    expect(file).toMatchObject({
      $id: "aw-1",
      name: "notes.pdf",
      type: "document",
      extension: "pdf",
      size: 120,
      storageBackend: "appwrite",
      owner: { fullName: "Ada", email: "ada@example.com" },
    });
    expect(file.users).toEqual(["friend@example.com"]);
  });

  it("maps a MinIO DTO and keeps its backend label", () => {
    const file = toUnifiedMinio(minioDto);

    expect(file).toMatchObject({
      $id: "minio-1",
      name: "clip.mp4",
      type: "video",
      extension: "mp4",
      size: 240,
      storageBackend: "minio",
      owner: { fullName: "Grace", email: "grace@example.com" },
    });
    expect(file.users).toEqual(["friend@example.com"]);
  });

  it("normalizes unknown types to other", () => {
    const file = toUnifiedMinio({ ...minioDto, fileType: "archive" });

    expect(file.type).toBe("other");
  });
});

describe("sorting and merging", () => {
  const appwrite = makeFile({
    $id: "aw",
    name: "beta",
    size: 20,
    $createdAt: "2024-01-02T00:00:00.000Z",
  });
  const minio = makeFile({
    $id: "minio",
    name: "Alpha",
    size: 10,
    $createdAt: "2024-01-01T00:00:00.000Z",
    storageBackend: "minio",
  });

  it("keeps both sources and sorts by creation date by default", () => {
    const merged = mergeUnifiedFiles([appwrite], [minio]);

    expect(merged.map((file) => file.$id)).toEqual(["aw", "minio"]);
  });

  it("sorts by date ascending", () => {
    const merged = mergeUnifiedFiles([appwrite], [minio], "$createdAt-asc");

    expect(merged.map((file) => file.$id)).toEqual(["minio", "aw"]);
  });

  it("sorts by name case-insensitively using localeCompare", () => {
    const merged = mergeUnifiedFiles([appwrite], [minio], "name-asc");

    expect(merged.map((file) => file.name)).toEqual(["Alpha", "beta"]);
  });

  it("sorts by size descending", () => {
    const merged = mergeUnifiedFiles([appwrite], [minio], "size-desc");

    expect(merged.map((file) => file.size)).toEqual([20, 10]);
  });

  it("does not mutate the input arrays", () => {
    const appwriteInput = [appwrite];
    const minioInput = [minio];

    sortUnifiedFiles(appwriteInput, "name-desc");

    expect(appwriteInput).toEqual([appwrite]);
    expect(minioInput).toEqual([minio]);
  });
});

describe("space summaries", () => {
  it("starts empty with a 2GB total", () => {
    const summary = emptySpaceSummary();

    expect(summary.used).toBe(0);
    expect(summary.all).toBe(2 * 1024 * 1024 * 1024);
    expect(summary.available).toBe(true);
  });

  it("aggregates each type separately and tracks the newest file", () => {
    const summary = summarizeFiles([
      makeFile({
        type: "image",
        size: 100,
        $updatedAt: "2024-01-01T00:00:00.000Z",
      }),
      makeFile({
        type: "image",
        size: 50,
        $updatedAt: "2024-02-01T00:00:00.000Z",
      }),
      makeFile({
        type: "audio",
        size: 25,
        $updatedAt: "2024-03-01T00:00:00.000Z",
      }),
    ]);

    expect(summary.image.size).toBe(150);
    expect(summary.image.latestDate).toBe("2024-02-01T00:00:00.000Z");
    expect(summary.audio.size).toBe(25);
    expect(summary.document.size).toBe(0);
    expect(summary.used).toBe(175);
  });

  it("marks a backend unavailable without losing its total", () => {
    const summary = summarizeFiles([], 1024, false);

    expect(summary.available).toBe(false);
    expect(summary.all).toBe(1024);
  });
});

describe("helpers", () => {
  it("labels storage backends", () => {
    expect(storageBackendLabel("appwrite")).toBe("Appwrite");
    expect(storageBackendLabel("minio")).toBe("MinIO");
  });

  it("calculates percentages against a supplied total", () => {
    expect(calculatePercentage(512, 1024)).toBe(50);
    expect(calculatePercentage(0, 1024)).toBe(0);
    expect(calculatePercentage(512, 0)).toBe(0);
  });
});
