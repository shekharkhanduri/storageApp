import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { StorageBadge } from "@/components/files/StorageBadge";

describe("StorageBadge", () => {
  it("renders the Appwrite label", () => {
    render(<StorageBadge backend="appwrite" />);

    expect(screen.getByText("Appwrite")).toBeInTheDocument();
  });

  it("renders the MinIO label", () => {
    render(<StorageBadge backend="minio" />);

    expect(screen.getByText("MinIO")).toBeInTheDocument();
  });

  it("applies backend-specific styling", () => {
    const { rerender } = render(<StorageBadge backend="appwrite" />);

    expect(screen.getByText("Appwrite")).toHaveClass("text-sky-300");

    rerender(<StorageBadge backend="minio" />);

    expect(screen.getByText("MinIO")).toHaveClass("text-emerald-300");
  });
});
