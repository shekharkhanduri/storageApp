import { apiFetch } from "@/lib/api/client";

export interface BackendUser {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
}

export interface AuthPayload {
  accessToken: string;
  refreshToken?: string;
  user: BackendUser;
}

export interface AuthError {
  message?: string;
}

export const registerRequest = (input: {
  fullName: string;
  email: string;
  password?: string;
}) =>
  apiFetch<AuthPayload>("/api/auth/register", {
    method: "POST",
    body: input,
  });

export const loginRequest = (input: { email: string; password?: string }) =>
  apiFetch<AuthPayload>("/api/auth/login", {
    method: "POST",
    body: input,
  });

export const refreshRequest = (refreshToken: string) =>
  apiFetch<AuthPayload>("/api/auth/refresh", {
    method: "POST",
    refreshToken,
  });

export const meRequest = (accessToken: string) =>
  apiFetch<BackendUser>("/api/auth/me", {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });

export const logoutRequest = (accessToken: string) =>
  fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/auth/logout`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` },
  }).catch(() => null);
