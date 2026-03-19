import {Suspense} from "react";
import Link from "next/link";
import {getTranslations} from "next-intl/server";
import ProductCardSkeleton from "@/components/ProductCardSkeleton";
import MenuGridSection from "./MenuGridSection";

type Props = {
  params: Promise<{locale: string}>;
  searchParams: Promise<{
    category?: string;
    search?: string;
  }>;
};

function MenuGridFallback() {
  return (
    <>
      <div className="flex gap-2">
        {Array.from({length: 4}).map((_, i) => (
          <div
            key={i}
            className="h-10 w-24 rounded-xl bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]"
          />
        ))}
      </div>

      <div className="h-4 w-24 rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />

      <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({length: 6}).map((_, index) => (
          <ProductCardSkeleton key={index} />
        ))}
      </div>
    </>
  );
}

export default async function MenuPage({params, searchParams}: Props) {
  const {locale} = await params;
  const {category, search} = await searchParams;
  const t = await getTranslations("menu");

  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
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

        <form action={`/${locale}/menu`} className="flex gap-2">
          <input
            name="search"
            defaultValue={search}
            placeholder={t("searchPlaceholder")}
            className="w-full rounded-xl border px-4 py-2"
          />

          {category ? <input type="hidden" name="category" value={category} /> : null}

          <button
            type="submit"
            className="rounded-xl bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700"
          >
            {t("search")}
          </button>
        </form>
      </section>

      <Suspense
        key={`${category ?? "all"}-${search ?? ""}`}
        fallback={<MenuGridFallback />}
      >
        <MenuGridSection
          locale={locale}
          categoryParam={category}
          searchParam={search}
        />
      </Suspense>
    </main>
  );
}