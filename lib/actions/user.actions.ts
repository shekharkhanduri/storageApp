"use server";

import { cookies } from "next/headers";
import { avatarPlaceholderUrl } from "@/constants";
import { redirect } from "next/navigation";
import { parseStringify } from "@/lib/utils";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
} from "@/lib/api/client";
import {
  loginRequest,
  logoutRequest,
  meRequest,
  refreshRequest,
  registerRequest,
  type BackendUser,
} from "@/lib/api/auth.api";
import { clearAuthCookies, setAuthCookies } from "@/lib/auth/cookies";

const toCardUser = (user: BackendUser) =>
  parseStringify({
    $id: user.id,
    accountId: user.id,
    email: user.email,
    fullName: user.fullName,
    avatar: user.avatarUrl || avatarPlaceholderUrl,
  });

export const createAccount = async ({
  fullName,
  email,
  password,
}: {
  fullName: string;
  email: string;
  password?: string;
}) => {
  try {
    const { res, data } = await registerRequest({ fullName, email, password });

    if (!res.ok || !data) {
      return parseStringify({
        error:
          (data as { message?: string } | null)?.message ||
          "Registration failed",
      });
    }

    await setAuthCookies(data.accessToken, data.refreshToken);
    return parseStringify({ user: data.user });
  } catch (error) {
    console.error("Registration error:", error);
    return parseStringify({
      error: "Failed to connect to authentication server",
    });
  }
};

export const signInUser = async ({
  email,
  password,
}: {
  email: string;
  password?: string;
}) => {
  try {
    const { res, data } = await loginRequest({ email, password });

    if (!res.ok || !data) {
      return parseStringify({
        error:
          (data as { message?: string } | null)?.message ||
          "Invalid email or password",
      });
    }

    await setAuthCookies(data.accessToken, data.refreshToken);
    return parseStringify({ user: data.user });
  } catch (error) {
    console.error("Sign-in error:", error);
    return parseStringify({
      error: "Failed to connect to authentication server",
    });
  }
};

const fetchMeWithRefresh = async (
  accessToken: string | undefined,
  refreshToken: string | undefined,
) => {
  if (!accessToken && !refreshToken) return null;

  let token = accessToken;

  if (!token && refreshToken) {
    const { res, data } = await refreshRequest(refreshToken);
    if (!res.ok || !data?.accessToken) return null;
    token = data.accessToken;
    await setAuthCookies(token, data.refreshToken ?? refreshToken);
  }

  const { res, data } = await meRequest(token!);
  if (res.ok && data) return data;

  if (res.status === 401 && refreshToken) {
    const { res: refreshRes, data: refreshData } =
      await refreshRequest(refreshToken);
    if (!refreshRes.ok || !refreshData?.accessToken) return null;
    await setAuthCookies(
      refreshData.accessToken,
      refreshData.refreshToken ?? refreshToken,
    );
    const { res: retryRes, data: retryData } = await meRequest(
      refreshData.accessToken,
    );
    if (retryRes.ok && retryData) return retryData;
  }

  return null;
};

export const getCurrentUser = async () => {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
    const refreshToken = cookieStore.get(REFRESH_TOKEN_COOKIE)?.value;

    const user = await fetchMeWithRefresh(accessToken, refreshToken);
    if (!user) return null;

    return toCardUser(user);
  } catch (error) {
    console.error("Error fetching current user:", error);
    return null;
  }
};

export const signOutUser = async () => {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;

    if (accessToken) {
      await logoutRequest(accessToken);
    }

    await clearAuthCookies();
  } catch (error) {
    console.error("Sign-out error:", error);
  } finally {
    redirect("/sign-in");
  }
};
