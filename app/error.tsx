"use client";

import React, { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("App error:", error);
  }, [error]);

  return (
    <div className="flex h-screen flex-col items-center justify-center space-y-4 p-4 text-center">
      <h2 className="text-2xl font-bold text-dark-100">Something went wrong!</h2>
      <p className="max-w-md text-light-100">
        {error.message || "An unexpected error occurred. Please try again."}
      </p>
      <Button onClick={() => reset()} className="primary-btn">
        Try again
      </Button>
    </div>
  );
}
