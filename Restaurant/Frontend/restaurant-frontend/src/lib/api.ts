import type {Category, Product} from "@/types/product";

/**
 * Format a number to EUR currency (German format).
 */
export function formatPriceEUR(price: number) {
  return new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR"
  }).format(price);
}

/**
 * Build backend URL safely from NEXT_PUBLIC_API_URL.
 */
function backendUrl(path: string) {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!baseUrl) throw new Error("NEXT_PUBLIC_API_URL is not defined in .env.local");
  return new URL(path, baseUrl);
}

/**
 * Fetch products with optional filters.
 */
export async function getProducts(params?: {
  category?: string;
  bestSeller?: boolean;
}): Promise<Product[]> {
  const url = backendUrl("/api/products");

  if (params?.category) url.searchParams.set("category", params.category);
  if (params?.bestSeller) url.searchParams.set("bestSeller", "true");

  const res = await fetch(url.toString(), {cache: "no-store"});
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.status} ${res.statusText}`);

  return res.json();
}

/**
 * Fetch categories from the backend (server-driven).
 * Expected response: string[]
 */
export async function getCategories(): Promise<Category[]> {
  const url = backendUrl("/api/products/categories");

  const res = await fetch(url.toString(), {cache: "no-store"});
  if (!res.ok) throw new Error(`Failed to fetch categories: ${res.status} ${res.statusText}`);

  const data = (await res.json()) as Category[];

  // Deduplicate + normalize
  return Array.from(new Set(data.map((c) => String(c).trim()).filter(Boolean)));
}

/**
 * Convenience helper for best sellers.
 */
export async function getBestSellers(): Promise<Product[]> {
  return getProducts({bestSeller: true});
}