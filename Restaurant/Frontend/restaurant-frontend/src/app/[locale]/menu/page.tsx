import Link from "next/link";
import {getTranslations} from "next-intl/server";
import {Product} from "../../../types/product";

type CategoryKey = "milk_tea" | "fruit_tea" |"coffee" | "chicken";

function normalizeCategory(input?: string): CategoryKey | undefined {
  const value = (input ?? "").toLowerCase().trim();
  if (value === "milk_tea" || value == "fruit_tea" || value === "coffee" || value === "chicken") {
    return value;
  }
  return undefined;
}

/**
 * Fetch products from Java backend.
 * Backend already returns localized name/description.
 */
async function fetchProducts(category?: CategoryKey): Promise<Product[]> {
  const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  if (!baseUrl) {
    throw new Error("NEXT_PUBLIC_API_URL is not defined");
  }

  const url = new URL("/api/products", baseUrl);
  if (category) {
    url.searchParams.set("category", category);
  }

  const res = await fetch(url.toString(), {cache: "no-store"});

  if (!res.ok) {
    throw new Error(`Failed to fetch products: ${res.status}`);
  }

  return res.json();
}

function formatPriceEUR(price: number) {
  return new Intl.NumberFormat("de-DE", {
    style: "currency",
    currency: "EUR"
  }).format(price);
}

/**
 * Menu page (Server Component).
 * - Category filter via query string
 * - Product name/description come directly from backend
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

  const category = normalizeCategory(sp.category);
  const query = (sp.q ?? "").toLowerCase().trim();

  const products = await fetchProducts(category);

  const filtered = query
    ? products.filter(
        (p) =>
          p.name.toLowerCase().includes(query) ||
          (p.description ?? "").toLowerCase().includes(query)
      )
    : products;

  const tabs = [
    {label: t("tabAll"), href: `/${locale}/menu`},
    {label: t("tabBubbleTea"), href: `/${locale}/menu?category=milk_tea`},
    {label: t("tabBubbleTea"), href: `/${locale}/menu?category=fruit_tea`},
    {label: t("tabCoffee"), href: `/${locale}/menu?category=coffee`},
    {label: t("tabChicken"), href: `/${locale}/menu?category=chicken`}
  ];

  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
      {/* ================= HEADER ================= */}
      <section className="space-y-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-extrabold text-gray-900">
              {t("title")}
            </h1>
            <p className="mt-1 text-gray-600">{t("subtitle")}</p>
          </div>

          <Link
            href={`/${locale}/checkout`}
            className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {t("goToCheckout")} →
          </Link>
        </div>

        {/* ================= CATEGORY TABS ================= */}
        <div className="flex flex-wrap gap-2">
          {tabs.map((tab) => (
            <Link
              key={tab.href}
              href={tab.href}
              className={`rounded-full px-4 py-2 text-sm font-semibold ${
                tab.href.includes(category ?? "")
                  ? "bg-blue-600 text-white"
                  : "border bg-white text-blue-700 hover:bg-blue-50"
              }`}
            >
              {tab.label}
            </Link>
          ))}
        </div>

        {/* ================= SEARCH ================= */}
        <form action={`/${locale}/menu`} className="flex gap-3">
          {category && <input type="hidden" name="category" value={category} />}
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
          <article
            key={p.id}
            className="rounded-3xl border bg-white p-6 shadow-sm"
          >
            {/* Category icon */}
            <div className="mb-4 flex h-32 items-center justify-center rounded-2xl bg-blue-50 text-3xl">
              {p.category === "bubbletea" && "🧋"}
              {p.category === "coffee" && "☕"}
              {p.category === "chicken" && "🍗"}
            </div>

            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">
                  {p.name}
                </h3>
                {p.description && (
                  <p className="mt-2 text-sm text-gray-600">
                    {p.description}
                  </p>
                )}
              </div>

              <div className="rounded-full bg-blue-600 px-3 py-1 text-sm font-bold text-white">
                {formatPriceEUR(p.price)}
              </div>
            </div>

            {p.bestSeller && (
              <div className="mt-3 inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                {t("bestSeller")}
              </div>
            )}

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
          </article>
        ))}
      </section>

      {/* ================= EMPTY STATE ================= */}
      {filtered.length === 0 && (
        <section className="rounded-3xl border bg-white p-10 text-center">
          <h2 className="text-xl font-extrabold text-gray-900">
            {t("emptyTitle")}
          </h2>
          <p className="mt-2 text-gray-600">{t("emptySubtitle")}</p>
          <Link
            href={`/${locale}/menu`}
            className="mt-6 inline-flex rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
          >
            {t("reset")}
          </Link>
        </section>
      )}
    </main>
  );
}
