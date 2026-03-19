import Link from "next/link";
import {getTranslations} from "next-intl/server";
import type {Category, Product} from "@/types/product";
import ProductCard from "@/components/ProductCard";
import { getCategories, getProducts } from "@/lib/product-api";

function validateCategory(input: string | undefined, categories: Category[]): Category | undefined {
  const value = (input ?? "").trim();
  if (!value) return undefined;

  return categories.find((c) => c.toLowerCase() === value.toLowerCase());
}

type Props = {
  locale: string;
  categoryParam?: string;
  searchParam?: string;
};

export default async function MenuGridSection({
  locale,
  categoryParam,
  searchParam
}: Props) {
  const t = await getTranslations("menu");

  const categories = await getCategories();
  const category = validateCategory(categoryParam, categories);

  const products: Product[] = await getProducts({category});

  const query = (searchParam ?? "").toLowerCase().trim();
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
    <>
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

      <div className="text-sm text-gray-600">{t("results", {count: filtered.length})}</div>

      {filtered.length > 0 ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((product) => (
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
          <h3 className="text-lg font-semibold text-gray-900">{t("emptyTitle")}</h3>
          <p className="mt-2 text-gray-600">{t("emptySubtitle")}</p>

          <Link
            href={`/${locale}/menu`}
            className="mt-4 inline-block rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700"
          >
            {t("reset")}
          </Link>
        </div>
      )}
    </>
  );
}