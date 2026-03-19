import type {Category, Product} from "@/types/product";
import {backendUrl, readJsonOrThrow} from "@/lib/http";

/**
 * Fetch all products or filter by category / bestSeller.
 */
export async function getProducts(params?: {
  category?: string;
  bestSeller?: boolean;
}): Promise<Product[]> {
  const url = backendUrl("/api/products");

  if (params?.category) {
    url.searchParams.set("category", params.category);
  }

  if (params?.bestSeller) {
    url.searchParams.set("bestSeller", "true");
  }

  const res = await fetch(url.toString(), {
    cache: "no-store"
  });

  return readJsonOrThrow<Product[]>(res, "Failed to fetch products");
}

/**
 * Convenience helper for best sellers.
 */
export async function getBestSellers(): Promise<Product[]> {
  return getProducts({bestSeller: true});
}

/**
 * Fetch product categories from backend.
 * Expected response: string[]
 */
export async function getCategories(): Promise<Category[]> {
  const url = backendUrl("/api/products/categories");

  const res = await fetch(url.toString(), {
    cache: "no-store"
  });

  if (!res.ok) {
    console.error(`Failed to fetch categories: ${res.status} ${res.statusText}`);
    return [];
  }

  const data = (await res.json()) as Category[];

  return Array.from(new Set(data.map((c) => String(c).trim()).filter(Boolean)));
}