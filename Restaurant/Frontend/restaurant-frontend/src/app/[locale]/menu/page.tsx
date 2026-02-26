import Link from "next/link";
import {getTranslations} from "next-intl/server";
import {formatPriceEUR, getCategories, getProducts} from "@/lib/api";
import type {Category, Product} from "@/types/product";

/**
 * Validate that the category from URL exists in the server-driven category list.
 */
function validateCategory(input: string | undefined, categories: Category[]): Category | undefined {
  const value = (input ?? "").trim();
  if (!value) return undefined;

  // Case-insensitive match against server list
  const found = categories.find((c) => c.toLowerCase() === value.toLowerCase());
  return found;
}

/**
 * Menu page (Server Component).
 * - Category list is fetched from server
 * - Category filter is validated against that list
 * - Products are fetched from server (no local data)
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

  // Fetch server-driven categories
  const categories = await getCategories();

  // Validate category from query param against server list
  const category = validateCategory(sp.category, categories);

  // Fetch products (server)
  const products: Product[] = await getProducts({category});

  // Server-side search (simple, no client state needed)
  const query = (sp.q ?? "").toLowerCase().trim();
  const filtered = query
    ? products.filter(
        (p) =>
          p.name.toLowerCase().includes(query) ||
          (p.description ?? "").toLowerCase().includes(query)
      )
    : products;

  // Build tabs dynamically from server categories
  const tabs: Array<{label: string; href: string; active: boolean}> = [
    {
      label: t("tabAll"),
      href: `/${locale}/menu`,
      active: !category
    },
    ...categories.map((c) => ({
      // Label strategy:
      // - If you want localized labels, use messages keys like menu.categoryLabels.<category>
      // - For now, display the category string as-is.
      label: c,
      href: `/${locale}/menu?category=${encodeURIComponent(c)}`,
      active: !!category && c.toLowerCase() === category.toLowerCase()
    }))
  ];

  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
      {/* ================= HEADER ================= */}
      <section className="space-y-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold text-gray-900">{t("title")}</h1>
            <p className="mt-1 text-gray-600">{t("subtitle")}</p>
          </div>

          <Link
            href={`/${locale}/checkout`}
            className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("goToCheckout")} →
          </Link>
        </div>

        {/* ================= TABS (SERVER-DRIVEN) ================= */}
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => (
            <Link
              key={tab.href}
              href={tab.href}
              className={[
                "rounded-full px-4 py-2 text-sm font-semibold",
                tab.active ? "bg-blue-600 text-white" : "border bg-white text-blue-700 hover:bg-blue-50"
              ].join(" ")}
            >
              {tab.label}
            </Link>
          ))}
        </div>

        {/* ================= SEARCH ================= */}
        <form action={`/${locale}/menu`} className="flex gap-3">
          {/* Keep category when searching */}
          {category ? <input type="hidden" name="category" value={category} /> : null}

          <input
            name="q"
            defaultValue={sp.q ?? ""}
            placeholder={t("searchPlaceholder")}
            className="flex-1 rounded-2xl border px-4 py-3 text-sm outline-none ring-blue-200 focus:ring-4"
          />

          <button
            type="submit"
            className="rounded-2xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("search")}
          </button>
        </form>
      </section>

      {/* ================= PRODUCTS GRID ================= */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((p) => (
          <article key={p.id} className="rounded-3xl border bg-white p-6 shadow-sm">
            <div className="mb-4 flex h-32 items-center justify-center rounded-2xl bg-blue-50 text-3xl">
              🧋
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
              <button
                type="button"
                className="flex-1 rounded-2xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
              >
                {t("addToCart")}
              </button>

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
            {t("reset")} →
          </Link>
        </section>
      ) : null}
    </main>
  );
}