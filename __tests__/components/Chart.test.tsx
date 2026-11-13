import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Chart } from "@/components/dashboard/Chart";

describe("Chart", () => {
  it("renders a storage-specific label and totals", () => {
    const { container } = render(
      <Chart used={512} total={1024} label="MinIO" color="#2dd4bf" />,
    );

    expect(screen.getByText("MinIO Storage")).toBeInTheDocument();
    expect(container.querySelector(".chart-description")).toHaveTextContent(
      "512 Bytes / 1.0 KB",
    );
  });

  it("renders zero usage safely", () => {
    const { container } = render(
      <Chart used={0} total={1024} label="Appwrite" />,
    );

    expect(screen.getByText("Appwrite Storage")).toBeInTheDocument();
    expect(container.querySelector(".chart-description")).toHaveTextContent(
      "0 Bytes / 1.0 KB",
    );
  });
});
