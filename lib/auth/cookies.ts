"use server";

import { cookies } from "next/headers";
import {
  ACCESS_TOKEN_COOKIE,
  ACCESS_TOKEN_MAX_AGE,
  REFRESH_TOKEN_COOKIE,
  REFRESH_TOKEN_MAX_AGE,
} from "@/lib/api/client";

const baseOptions = {
  path: "/",
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
};

export const setAuthCookies = async (
  accessToken: string,
  refreshToken?: string,
) => {
  try {
    const cookieStore = await cookies();

    cookieStore.set(ACCESS_TOKEN_COOKIE, accessToken, {
      ...baseOptions,
      maxAge: ACCESS_TOKEN_MAX_AGE,
    });

    if (refreshToken) {
      cookieStore.set(REFRESH_TOKEN_COOKIE, refreshToken, {
        ...baseOptions,
        maxAge: REFRESH_TOKEN_MAX_AGE,
      });
    }
  } catch {
    // Next.js restricts cookie mutation during Server Component rendering.
    // In Server Actions or Route Handlers, cookieStore.set succeeds.
    // In Server Components, we catch and continue with the in-memory refreshed token.
  }
};

export const clearAuthCookies = async () => {
  try {
    const cookieStore = await cookies();
    cookieStore.delete(ACCESS_TOKEN_COOKIE);
    cookieStore.delete(REFRESH_TOKEN_COOKIE);
  } catch {
    // Next.js restricts cookie mutation during Server Component rendering.
  }
};
