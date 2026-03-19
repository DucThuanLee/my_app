import type {AuthResponse, LoginRequest, RegisterRequest} from "@/types/auth";
import {backendUrl, readJsonOrThrow} from "@/lib/http";

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const url = backendUrl("/auth/register");

  const res = await fetch(url.toString(), {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  return readJsonOrThrow<AuthResponse>(res, "Failed to register");
}

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const url = backendUrl("/auth/login");

  const res = await fetch(url.toString(), {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  return readJsonOrThrow<AuthResponse>(res, "Failed to login");
}