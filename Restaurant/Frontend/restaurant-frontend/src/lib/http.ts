import {getAccessToken} from "@/lib/auth-storage";

export function backendUrl(path: string) {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;

  if (!baseUrl) {
    throw new Error("NEXT_PUBLIC_API_URL is not defined in .env.local");
  }

  return new URL(path, baseUrl);
}

export async function readJsonOrThrow<T>(res: Response, message: string): Promise<T> {
  if (!res.ok) {
    let details = "";

    try {
      details = await res.text();
    } catch {
      details = "";
    }

    throw new Error(
      details
        ? `${message}: ${res.status} ${res.statusText} - ${details}`
        : `${message}: ${res.status} ${res.statusText}`
    );
  }

  return res.json() as Promise<T>;
}

export function formatPriceEUR(price: number) {
  return new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR"
  }).format(price);
}

export function buildAuthHeaders(extra?: HeadersInit): HeadersInit {
  const token = getAccessToken();

  return {
    ...(extra ?? {}),
    ...(token ? {Authorization: `Bearer ${token}`} : {})
  };
}