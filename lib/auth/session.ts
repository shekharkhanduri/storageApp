import { cookies } from "next/headers";

import { refreshRequest } from "@/lib/api/auth.api";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
} from "@/lib/api/client";
import { setAuthCookies } from "@/lib/auth/cookies";

export const getAccessToken = async (): Promise<string | null> => {
  const cookieStore = await cookies();
  return cookieStore.get(ACCESS_TOKEN_COOKIE)?.value ?? null;
};

export const refreshAccessToken = async (): Promise<string | null> => {
  const cookieStore = await cookies();
  const refreshToken = cookieStore.get(REFRESH_TOKEN_COOKIE)?.value;

  if (!refreshToken) return null;

  const { res, data } = await refreshRequest(refreshToken);

  if (!res.ok || !data?.accessToken) return null;

  await setAuthCookies(data.accessToken, data.refreshToken);

  return data.accessToken;
};
