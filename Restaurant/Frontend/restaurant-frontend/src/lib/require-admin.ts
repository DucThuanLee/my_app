// src/lib/require-admin.ts
import {getAccessToken} from "@/lib/auth-storage";
import {getEmailFromToken, getRoleFromToken} from "@/lib/jwt";

export function requireAdmin() {
  const token = getAccessToken();

  if (!token) {
    throw new Error("UNAUTHORIZED");
  }

  const role = getRoleFromToken(token);

  if (role !== "ADMIN") {
    throw new Error("FORBIDDEN");
  }

  return {token};
}