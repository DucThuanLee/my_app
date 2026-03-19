"use client";

import Link from "next/link";
import AddToCartButton from "@/components/AddToCartButton";
import {formatPriceEUR} from "@/lib/api";
import type {Product} from "@/types/product";

type Props = {
  product: Product;
  locale: string;
  addToCartLabel: string;
  addedLabel?: string;
  detailsLabel: string;
  bestSellerLabel?: string;
  categoryLabel?: string;
};

/**
 * Return a visual emoji for a product category.
 * This is only used for presentation when no real product image exists yet.
 */
function getCategoryEmoji(category: string) {
  const key = category.toLowerCase();

  if (key.includes("bubble")) return "🧋";
  if (key.includes("coffee")) return "☕";
  if (key.includes("chicken") || key.includes("hähn") || key.includes("hahn")) return "🍗";

  return "🍽️";
}

/**
 * Production-ready product card.
 * - Reusable across home, menu and future listing pages
 * - Uses shared AddToCartButton
 * - Keeps layout clean and consistent
 */
export default function ProductCard({
  product,
  locale,
  addToCartLabel,
  addedLabel,
  detailsLabel,
  bestSellerLabel = "Best Seller",
  categoryLabel = "Category"
}: Props) {
  const emoji = getCategoryEmoji(product.category);

  return (
    <article className="group rounded-3xl border bg-white p-6 shadow-sm transition duration-200 hover:-translate-y-0.5 hover:shadow-md">
      <div className="mb-4 flex h-36 items-center justify-center rounded-2xl bg-blue-50 text-4xl">
        <span aria-hidden>{emoji}</span>
      </div>

      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          {product.bestSeller ? (
            <div className="mb-2 inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
              {bestSellerLabel}
            </div>
          ) : null}

          <h3 className="text-lg font-semibold text-gray-900 transition group-hover:text-blue-700">
            {product.name}
          </h3>

          {product.description ? (
            <p className="mt-2 line-clamp-2 text-sm leading-6 text-gray-600">
              {product.description}
            </p>
          ) : null}
        </div>

        <div className="shrink-0 rounded-full bg-blue-600 px-3 py-1 text-sm font-bold text-white">
          {formatPriceEUR(product.price)}
        </div>
      </div>

      <div className="mt-4 text-xs font-semibold text-gray-500">
        {categoryLabel}: <span className="text-blue-700">{product.category}</span>
      </div>

      <div className="mt-5 flex gap-3">
        <AddToCartButton
          product={product}
          label={addToCartLabel}
          addedLabel={addedLabel}
        />

        <Link
          href={`/${locale}/menu?category=${encodeURIComponent(product.category)}`}
          className="inline-flex items-center justify-center rounded-2xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
        >
          {detailsLabel}
        </Link>
      </div>
    </article>
  );
}