import Link from "next/link";
import { getTranslations } from "next-intl/server";

import type { Category, Product } from "@/types/product";

import AddToCartButton from "@/components/AddToCartButton";
import ProductCard from "@/components/ProductCard";
import { getBestSellers, getCategories } from "@/lib/product-api";
import { formatPriceEUR } from "@/lib/http";

/**
 * Pick an emoji for a given category key.
 * This is only for visual presentation.
 */
function categoryEmoji(category: string) {
  const key = category.toLowerCase();

  if (key.includes("bubble")) return "🧋";
  if (key.includes("coffee")) return "☕";
  if (key.includes("chicken") || key.includes("hähn") || key.includes("hahn")) return "🍗";

  return "🍽️";
}

export default async function HomePage({
  params
}: {
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  const t = await getTranslations("home");

  const [categories, bestSellers] = await Promise.all([
    getCategories(),
    getBestSellers()
  ]);

  const popularToday: Product[] = bestSellers.slice(0, 3);

  return (
    <main className="mx-auto max-w-6xl space-y-14 px-4 pb-24 pt-8 md:pb-16">
      {/* HERO */}
      <section className="relative overflow-hidden rounded-3xl border bg-white p-8 md:p-12">
        <div className="pointer-events-none absolute -top-24 -right-24 h-72 w-72 rounded-full bg-blue-200/60 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-sky-200/60 blur-3xl" />

        <div className="relative grid gap-10 md:grid-cols-2 md:items-center">
          <div className="space-y-5">
            <span className="inline-flex items-center gap-2 rounded-full bg-blue-50 px-4 py-1 text-sm font-semibold text-blue-700">
              {t("badge")}
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-blue-600" />
              {t("badge2")}
            </span>

            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 md:text-5xl">
              {t("heroTitle")}
              <br />
              <span className="text-blue-700">{t("heroTitle2")}</span>
            </h1>

            <p className="max-w-prose text-lg leading-relaxed text-gray-600">
              {t("heroSubtitle")}
            </p>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link
                href={`/${locale}/menu`}
                className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white shadow-sm hover:bg-blue-700"
              >
                {t("ctaMenu")}
              </Link>

              <Link
                href={`/${locale}/cart`}
                className="inline-flex items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("ctaOrder")}
              </Link>
            </div>

            <div className="flex flex-wrap gap-2 text-sm text-gray-600">
              <span className="rounded-full border bg-white px-3 py-1">
                ✓ {t("perkFast")}
              </span>
              <span className="rounded-full border bg-white px-3 py-1">
                ✓ {t("perkGuest")}
              </span>
              <span className="rounded-full border bg-white px-3 py-1">
                ✓ {t("perkPayment")}
              </span>
            </div>
          </div>

          {/* Popular today */}
          <div className="relative">
            <div className="rounded-3xl border bg-gradient-to-b from-blue-50 to-white p-6 shadow-sm">
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-semibold text-blue-700">
                    {t("popularToday")}
                  </span>
                  <span className="rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold text-white">
                    {t("bestSellerTag")}
                  </span>
                </div>

                <div className="space-y-3">
                  {popularToday.length > 0 ? (
                    popularToday.map((item) => (
                      <div
                        key={item.id}
                        className="flex items-center justify-between rounded-2xl border bg-white p-4"
                      >
                        <div className="min-w-0">
                          <div className="truncate font-semibold text-gray-900">
                            {item.name}
                          </div>
                          {item.description ? (
                            <div className="mt-1 line-clamp-1 text-xs text-gray-500">
                              {item.description}
                            </div>
                          ) : null}
                        </div>

                        <span className="ml-3 shrink-0 font-bold text-blue-700">
                          {formatPriceEUR(item.price)}
                        </span>
                      </div>
                    ))
                  ) : (
                    <div className="rounded-2xl border bg-white p-4 text-sm text-gray-600">
                      {t("noBestSellers")}
                    </div>
                  )}
                </div>

                <div className="rounded-2xl bg-blue-600 p-4 text-white">
                  <div className="text-sm opacity-90">{t("etaLabel")}</div>
                  <div className="text-lg font-bold">{t("etaValue")}</div>
                </div>
              </div>
            </div>

            <div className="pointer-events-none absolute -bottom-8 -right-8 h-28 w-28 rounded-full bg-blue-300/50 blur-2xl" />
          </div>
        </div>
      </section>

      {/* CATEGORIES */}
      <section className="space-y-5">
        <div className="flex items-end justify-between">
          <div>
            <h2 className="text-2xl font-extrabold text-gray-900">
              {t("categories")}
            </h2>
            <p className="mt-1 text-gray-600">{t("categoriesSubtitle")}</p>
          </div>

          <Link
            href={`/${locale}/menu`}
            className="hidden rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:inline-flex"
          >
            {t("seeAll")} →
          </Link>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          {(categories as Category[]).map((c) => (
            <div key={c} className="rounded-3xl border bg-white p-6 shadow-sm">
              <div className="flex items-start justify-between">
                <h3 className="text-lg font-semibold text-blue-700">{c}</h3>
                <span className="text-2xl" aria-hidden>
                  {categoryEmoji(c)}
                </span>
              </div>

              <p className="mt-2 text-gray-600">{t("categoryCardHint")}</p>

              <Link
                href={`/${locale}/menu?category=${encodeURIComponent(c)}`}
                className="mt-4 inline-flex items-center gap-2 font-semibold text-blue-700 hover:underline"
              >
                {t("explore")} <span aria-hidden>→</span>
              </Link>
            </div>
          ))}
        </div>

        {categories.length === 0 ? (
          <div className="rounded-3xl border bg-white p-8 text-sm text-gray-600">
            {t("noCategories")}
          </div>
        ) : null}

        <Link
          href={`/${locale}/menu`}
          className="inline-flex rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:hidden"
        >
          {t("seeAll")} →
        </Link>
      </section>

      {/* BEST SELLERS */}
      <section className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div>
            <h2 className="text-2xl font-extrabold text-gray-900">
              {t("bestSellersTitle")}
            </h2>
            <p className="mt-1 text-gray-600">{t("bestSellersSubtitle")}</p>
          </div>

          <Link
            href={`/${locale}/menu`}
            className="hidden rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:inline-flex"
          >
            {t("seeAll")} →
          </Link>
        </div>

        {bestSellers.length > 0 ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {bestSellers.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                locale={locale}
                addToCartLabel={t("addToCart")}
                addedLabel={t("addedToCart")}
                detailsLabel={t("details")}
                bestSellerLabel={t("bestSellerTag")}
                categoryLabel={t("categoryLabel")}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-3xl border bg-white p-8 text-sm text-gray-600">
            {t("noBestSellers")}
          </div>
        )}

        <Link
          href={`/${locale}/menu`}
          className="inline-flex rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:hidden"
        >
          {t("seeAll")} →
        </Link>
      </section>

      {/* QUICK ORDER STRIP */}
      <section className="rounded-3xl border bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 className="text-xl font-extrabold text-gray-900">
              {t("bestSellersTitle")}
            </h2>
            <p className="mt-1 text-gray-600">{t("bestSellersSubtitle")}</p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href={`/${locale}/menu`}
              className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50"
            >
              {t("ctaMenu")}
            </Link>

            <Link
              href={`/${locale}/cart`}
              className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
            >
              {t("ctaOrder")}
            </Link>
          </div>
        </div>
      </section>

      {/* MOBILE STICKY CTA */}
      <div className="fixed inset-x-0 bottom-0 z-50 border-t bg-white/90 p-3 backdrop-blur md:hidden">
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-4">
          <Link
            href={`/${locale}/menu`}
            className="inline-flex flex-1 items-center justify-center rounded-2xl border px-4 py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50"
          >
            {t("ctaMenu")}
          </Link>

          <Link
            href={`/${locale}/cart`}
            className="inline-flex flex-1 items-center justify-center rounded-2xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("ctaOrder")}
          </Link>
        </div>
      </div>
    </main>
  );
}