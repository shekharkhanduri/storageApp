import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import Card from "@/components/files/Card";

vi.mock("@/components/files/ActionDropdown", () => ({
  default: () => <span data-testid="actions" />,
}));

const file: UnifiedFile = {
  $id: "file-1",
  name: "photo.png",
  type: "image",
  extension: "png",
  size: 2048,
  url: "http://localhost:9000/storeit-files/owner/photo.png",
  $createdAt: "2024-05-01T10:00:00.000Z",
  $updatedAt: "2024-05-01T10:00:00.000Z",
  owner: { fullName: "Grace" },
  users: [],
  storageBackend: "minio",
};

describe("Card", () => {
  it("renders a MinIO card with its badge and owner", () => {
    render(<Card file={file} />);

    expect(screen.getByText("photo.png")).toBeInTheDocument();
    expect(screen.getByText("MinIO")).toBeInTheDocument();
    expect(screen.getByText("By: Grace")).toBeInTheDocument();
    expect(screen.getByText("2.0 KB")).toBeInTheDocument();
  });

  it("renders an Appwrite card with its badge", () => {
    render(
      <Card
        file={{
          ...file,
          $id: "file-2",
          name: "notes.pdf",
          type: "document",
          extension: "pdf",
          storageBackend: "appwrite",
        }}
      />,
    );

    expect(screen.getByText("Appwrite")).toBeInTheDocument();
  });
});
