export type JwtPayload = {
    iss?: string;
    sub?: string;
    exp?: number;
    iat?: number;
    role?: string;
    [key: string]: unknown;
  };
  
  function decodeBase64Url(input: string) {
    const base64 = input.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(
      base64.length + ((4 - (base64.length % 4)) % 4),
      "="
    );
  
    if (typeof window === "undefined") {
      return Buffer.from(padded, "base64").toString("utf-8");
    }
  
    return decodeURIComponent(
      Array.from(atob(padded))
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, "0")}`)
        .join("")
    );
  }
  
  export function decodeJwt(token: string): JwtPayload | null {
    try {
      const parts = token.split(".");
      if (parts.length < 2) return null;
  
      const payload = decodeBase64Url(parts[1]);
      return JSON.parse(payload) as JwtPayload;
    } catch {
      return null;
    }
  }
  
  export function getEmailFromToken(token: string | null | undefined) {
    if (!token) return null;
    const payload = decodeJwt(token);
    return typeof payload?.sub === "string" ? payload.sub : null;
  }
  
  export function getRoleFromToken(token: string | null | undefined) {
    if (!token) return null;
    const payload = decodeJwt(token);
    return typeof payload?.role === "string" ? payload.role : null;
  }
  
  export function getInitialFromEmail(email: string | null | undefined) {
    if (!email) return "A";
    const trimmed = email.trim();
    if (!trimmed) return "A";
    return trimmed.charAt(0).toUpperCase();
  }
  
  export function isTokenExpired(token: string | null | undefined) {
    if (!token) return true;
  
    const payload = decodeJwt(token);
    if (!payload?.exp || typeof payload.exp !== "number") {
      return true;
    }
  
    const nowInSeconds = Math.floor(Date.now() / 1000);
    return payload.exp <= nowInSeconds;
  }