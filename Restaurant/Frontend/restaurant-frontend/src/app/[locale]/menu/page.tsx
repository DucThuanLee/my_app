import Link from "next/link";
import {getTranslations} from "next-intl/server";
import {formatPriceEUR, getCategories, getProducts} from "@/lib/api";
import type {Category, Product} from "@/types/product";
import AddToCartButton from "@/components/AddToCartButton";

/**
 * Validate that the category from URL exists in the server-driven category list.
 */
function validateCategory(input: string | undefined, categories: Category[]): Category | undefined {
  const value = (input ?? "").trim();
  if (!value) return undefined;

  const found = categories.find((c) => c.toLowerCase() === value.toLowerCase());
  return found;
}

/**
 * Pick a visual emoji for category cards/items.
 */
function categoryEmoji(category: string) {
  const key = category.toLowerCase();

  if (key.includes("bubble")) return "🧋";
  if (key.includes("coffee")) return "☕";
  if (key.includes("chicken") || key.includes("hähn") || key.includes("hahn")) return "🍗";

  return "🍽️";
}

/**
 * Menu page (Server Component).
 * - Categories come from server
 * - Products come from server
 * - Search/filter is handled server-side
 * - Shared header/footer are rendered in [locale]/layout.tsx
 */
export default async function MenuPage({
  params,
  searchParams
}: {
  params: Promise<{locale: string}>;
  searchParams: Promise<{category?: string; q?: string}>;
}) {
  const {locale} = await params;
  const sp = await searchParams;

  const t = await getTranslations("menu");

  const categories = await getCategories();
  const category = validateCategory(sp.category, categories);

  const products: Product[] = await getProducts({category});

  const query = (sp.q ?? "").toLowerCase().trim();
  const filtered = query
    ? products.filter(
        (p) =>
          p.name.toLowerCase().includes(query) ||
          (p.description ?? "").toLowerCase().includes(query)
      )
    : products;

  const tabs: Array<{label: string; href: string; active: boolean}> = [
    {
      label: t("tabAll"),
      href: `/${locale}/menu`,
      active: !category
    },
    ...categories.map((c) => ({
      label: c,
      href: `/${locale}/menu?category=${encodeURIComponent(c)}`,
      active: !!category && c.toLowerCase() === category.toLowerCase()
    }))
  ];

  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
      {/* ================= PAGE HEADER ================= */}
      <section className="space-y-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold text-gray-900">{t("title")}</h1>
            <p className="mt-1 text-gray-600">{t("subtitle")}</p>
          </div>

          <Link
            href={`/${locale}/cart`}
            className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("goToCart")} →
          </Link>
        </div>

        {/* ================= CATEGORY TABS ================= */}
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => (
            <Link
              key={tab.href}
              href={tab.href}
              className={[
                "rounded-full px-4 py-2 text-sm font-semibold",
                tab.active
                  ? "bg-blue-600 text-white"
                  : "border bg-white text-blue-700 hover:bg-blue-50"
              ].join(" ")}
            >
              {tab.label}
            </Link>
          ))}
        </div>

        {/* ================= SEARCH ================= */}
        <form action={`/${locale}/menu`} className="flex gap-3">
          {category ? <input type="hidden" name="category" value={category} /> : null}

          <input
            name="q"
            defaultValue={sp.q ?? ""}
            placeholder={t("searchPlaceholder")}
            className="flex-1 rounded-2xl border bg-white px-4 py-3 text-sm outline-none ring-blue-200 focus:ring-4"
          />

          <button
            type="submit"
            className="rounded-2xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("search")}
          </button>
        </form>
      </section>

      {/* ================= RESULTS SUMMARY ================= */}
      <section className="flex items-center justify-between">
        <div className="text-sm text-gray-600">{t("results", {count: filtered.length})}</div>

        <Link
          href={`/${locale}`}
          className="text-sm font-semibold text-blue-700 hover:underline"
        >
          ← {t("backHome")}
        </Link>
      </section>

      {/* ================= PRODUCTS GRID ================= */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((p) => (
          <article key={p.id} className="rounded-3xl border bg-white p-6 shadow-sm">
            <div className="mb-4 flex h-32 items-center justify-center rounded-2xl bg-blue-50 text-3xl">
              {categoryEmoji(p.category)}
            </div>

            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">{p.name}</h3>

                {p.description ? (
                  <p className="mt-2 text-sm text-gray-600">{p.description}</p>
                ) : null}
              </div>

              <div className="rounded-full bg-blue-600 px-3 py-1 text-sm font-bold text-white">
                {formatPriceEUR(p.price)}
              </div>
            </div>

            {p.bestSeller ? (
              <div className="mt-3 inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                {t("bestSeller")}
              </div>
            ) : null}

            <div className="mt-5 flex gap-3">
              <AddToCartButton product={p} label={t("addToCart")} />

              <button
                type="button"
                className="rounded-2xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("details")}
              </button>
            </div>

            <div className="mt-4 text-xs font-semibold text-gray-500">
              {t("category")}: <span className="text-blue-700">{p.category}</span>
            </div>
          </article>
        ))}
      </section>

      {/* ================= EMPTY STATE ================= */}
      {filtered.length === 0 ? (
        <section className="rounded-3xl border bg-white p-10 text-center">
          <h2 className="text-xl font-extrabold text-gray-900">{t("emptyTitle")}</h2>
          <p className="mt-2 text-gray-600">{t("emptySubtitle")}</p>

          <Link
            href={`/${locale}/menu`}
            className="mt-6 inline-flex rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
          >
            {t("reset")}
          </Link>
        </section>
      ) : null}

      {/* ================= MOBILE STICKY CTA ================= */}
      <div className="fixed inset-x-0 bottom-0 z-50 border-t bg-white/90 p-3 backdrop-blur md:hidden">
        <div className="mx-auto flex max-w-6xl items-center gap-3 px-4">
          <Link
            href={`/${locale}/cart`}
            className="inline-flex flex-1 items-center justify-center rounded-2xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("goToCart")}
          </Link>
        </div>
      </div>
    </main>
  );
}