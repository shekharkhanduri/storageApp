import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import FileUploader from "@/components/files/FileUploader";

const mocks = vi.hoisted(() => ({
  uploadFile: vi.fn(),
  uploadFileToMinio: vi.fn(),
  toast: vi.fn(),
}));

const dropzone = vi.hoisted(() => ({
  onDrop: undefined as ((files: File[]) => Promise<void>) | undefined,
}));

vi.mock("@/lib/actions/file.actions", () => ({
  uploadFile: mocks.uploadFile,
}));

vi.mock("@/lib/files/minio-upload", () => ({
  uploadFileToMinio: mocks.uploadFileToMinio,
}));

vi.mock("@/hooks/use-toast", () => ({
  useToast: () => ({ toast: mocks.toast }),
}));

vi.mock("react-dropzone", () => ({
  useDropzone: ({ onDrop }: { onDrop: (files: File[]) => Promise<void> }) => {
    dropzone.onDrop = onDrop;
    return {
      getRootProps: () => ({}),
      getInputProps: () => ({}),
    };
  },
}));

const makeFile = () =>
  new File(["hello world"], "note.txt", { type: "text/plain" });

const dropFiles = async (files: File[]) => {
  await act(async () => {
    await dropzone.onDrop?.(files);
  });
};

describe("FileUploader", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    dropzone.onDrop = undefined;
  });

  it("uploads to Appwrite by default", async () => {
    render(<FileUploader ownerId="owner-1" accountId="account-1" />);

    mocks.uploadFile.mockResolvedValue({ $id: "uploaded" });

    const file = makeFile();
    await dropFiles([file]);

    expect(mocks.uploadFile).toHaveBeenCalledWith({
      file,
      ownerId: "owner-1",
      accountId: "account-1",
      path: "/",
    });
    expect(mocks.uploadFileToMinio).not.toHaveBeenCalled();
  });

  it("uploads to MinIO after switching the backend", async () => {
    render(<FileUploader ownerId="owner-1" accountId="account-1" />);

    fireEvent.click(screen.getByRole("button", { name: "minio" }));

    mocks.uploadFileToMinio.mockResolvedValue({ success: true });

    const file = makeFile();
    await dropFiles([file]);

    expect(mocks.uploadFileToMinio).toHaveBeenCalledWith({ file, path: "/" });
    expect(mocks.uploadFile).not.toHaveBeenCalled();
  });

  it("does not upload files larger than the limit", async () => {
    render(<FileUploader ownerId="owner-1" accountId="account-1" />);

    const oversized = new File(["x"], "large.bin");
    Object.defineProperty(oversized, "size", { value: 51 * 1024 * 1024 });

    await dropFiles([oversized]);

    expect(mocks.uploadFile).not.toHaveBeenCalled();
    expect(mocks.uploadFileToMinio).not.toHaveBeenCalled();
    expect(mocks.toast).toHaveBeenCalled();
  });

  it("keeps both storage buttons accessible and reports selection", () => {
    render(<FileUploader ownerId="owner-1" accountId="account-1" />);

    const appwrite = screen.getByRole("button", { name: "appwrite" });
    const minio = screen.getByRole("button", { name: "minio" });

    expect(appwrite).toHaveAttribute("aria-pressed", "true");
    expect(minio).toHaveAttribute("aria-pressed", "false");

    fireEvent.click(minio);

    expect(appwrite).toHaveAttribute("aria-pressed", "false");
    expect(minio).toHaveAttribute("aria-pressed", "true");
  });
});
