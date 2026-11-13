import "@testing-library/jest-dom/vitest";

import React from "react";
import { vi } from "vitest";

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}

globalThis.ResizeObserver =
  ResizeObserverMock as unknown as typeof ResizeObserver;

if (!URL.createObjectURL) {
  Object.defineProperty(URL, "createObjectURL", {
    value: vi.fn(() => "blob:mock"),
    writable: true,
  });
}

if (!URL.revokeObjectURL) {
  Object.defineProperty(URL, "revokeObjectURL", {
    value: vi.fn(),
    writable: true,
  });
}

vi.mock("next/image", () => ({
  __esModule: true,
  default: (props: Record<string, unknown>) => {
    const {
      src,
      alt,
      unoptimized: _unoptimized,
      priority: _priority,
      fill: _fill,
      ...rest
    } = props as {
      src: string;
      alt: string;
      unoptimized?: boolean;
      priority?: boolean;
      fill?: boolean;
    } & Record<string, unknown>;
    return React.createElement("img", { src, alt, ...rest });
  },
}));

vi.mock("next/link", () => ({
  __esModule: true,
  default: ({
    href,
    children,
    ...props
  }: {
    href: string;
    children: React.ReactNode;
  } & Record<string, unknown>) =>
    React.createElement("a", { href, ...props }, children),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/",
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    refresh: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    prefetch: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams(),
}));
