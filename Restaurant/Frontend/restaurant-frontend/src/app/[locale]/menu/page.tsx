import { getTranslations } from "next-intl/server";
import Link from "next/link";

import { getProducts, getCategories } from "@/lib/api";
import type { Product, Category } from "@/types/product";

import ProductCard from "@/components/ProductCard";

type Props = {
  params: Promise<{ locale: string }>;
  searchParams: Promise<{
    category?: string;
    search?: string;
  }>;
};

export default async function MenuPage({ params, searchParams }: Props) {
  const { locale } = await params;
  const { category, search } = await searchParams;

  const t = await getTranslations("menu");

  // Fetch server data
  const [products, categories] = await Promise.all([
    getProducts(),
    getCategories()
  ]);

  // Filter by category
  let filteredProducts: Product[] = products;

  if (category) {
    filteredProducts = filteredProducts.filter(
      (p) => p.category.toLowerCase() === category.toLowerCase()
    );
  }

  // Filter by search
  if (search) {
    const q = search.toLowerCase();

    filteredProducts = filteredProducts.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        (p.description?.toLowerCase().includes(q) ?? false)
    );
  }

  return (
    <main className="mx-auto max-w-6xl space-y-10 px-4 pb-24 pt-8">

      {/* PAGE HEADER */}
      <div className="space-y-2">
        <h1 className="text-3xl font-extrabold text-gray-900">
          {t("title")}
        </h1>

        <p className="text-gray-600">
          {t("subtitle")}
        </p>
      </div>

      {/* CATEGORY FILTER */}
      <div className="flex flex-wrap gap-2">
        <Link
          href={`/${locale}/menu`}
          className={`rounded-xl border px-4 py-2 text-sm font-semibold ${!category
            ? "bg-blue-600 text-white"
            : "text-gray-700 hover:bg-gray-50"
            }`}
        >
          {t("tabAll")}
        </Link>

        {(categories as Category[]).map((c) => (
          <Link
            key={c}
            href={`/${locale}/menu?category=${encodeURIComponent(c)}`}
            className={`rounded-xl border px-4 py-2 text-sm font-semibold ${category === c
              ? "bg-blue-600 text-white"
              : "text-gray-700 hover:bg-gray-50"
              }`}
          >
            {c}
          </Link>
        ))}
      </div>

      {/* SEARCH */}
      <form
        action={`/${locale}/menu`}
        method="GET"
        className="flex gap-2"
      >
        <input
          name="search"
          defaultValue={search}
          placeholder={t("searchPlaceholder")}
          className="w-full rounded-xl border px-4 py-2"
        />

        {category && (
          <input type="hidden" name="category" value={category} />
        )}

        <button
          type="submit"
          className="rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700"
        >
          {t("search")}
        </button>
      </form>

      {/* RESULT COUNT */}
      <div className="text-sm text-gray-500">
        {t("results", { count: filteredProducts.length })}
      </div>

      {/* PRODUCT GRID */}
      {filteredProducts.length > 0 ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filteredProducts.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              locale={locale}
              addToCartLabel={t("addToCart")}
              addedLabel={t("addedToCart")}
              detailsLabel={t("details")}
              bestSellerLabel={t("bestSeller")}
              categoryLabel={t("category")}
            />
          ))}
        </div>
      ) : (
        <div className="rounded-3xl border bg-white p-10 text-center">
          <h3 className="text-lg font-semibold text-gray-900">
            {t("emptyTitle")}
          </h3>

          <p className="mt-2 text-gray-600">
            {t("emptySubtitle")}
          </p>

          <Link
            href={`/${locale}/menu`}
            className="mt-4 inline-block rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700"
          >
            {t("reset")}
          </Link>
        </div>
      )}
    </main>
  );
}