import type { ReactNode } from "react";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations, setRequestLocale } from "next-intl/server";
import { hasLocale } from "next-intl";
import { notFound } from "next/navigation";
import Link from "next/link";
import LanguageSwitcher from "@/components/LanguageSwitcher";
import { routing } from "@i18n/routing";
import CartDrawer from "@/components/CartDrawer";


export default async function LocaleLayout({
  children,
  params
}: {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }

  setRequestLocale(locale);
  const messages = await getMessages();
  const t = await getTranslations("layout");

  return (
    <NextIntlClientProvider messages={messages}>
      <header className="sticky top-0 z-40 border-b bg-white/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link href={`/${locale}`} className="flex items-center gap-3">
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-blue-600 font-extrabold text-white shadow-sm">
              M
            </span>

            <div className="leading-tight">
              <div className="text-sm font-extrabold text-gray-900">
                {t("brand")}
              </div>
              <div className="text-xs text-gray-500">
                {t("tagline")}
              </div>
            </div>
          </Link>

          <nav className="hidden items-center gap-2 md:flex">
            <Link
              href={`/${locale}`}
              className="rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {t("navHome")}
            </Link>

            <Link
              href={`/${locale}/menu`}
              className="rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {t("navMenu")}
            </Link>

            <Link
              href={`/${locale}/menu`}
              className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
            >
              {t("orderNow")}
            </Link>
          </nav>

          <div className="flex items-center gap-2">
            <LanguageSwitcher locale={locale} />

            <CartDrawer
              locale={locale}
              title={t("cart")}
              emptyTitle={t("cartEmptyTitle")}
              emptySubtitle={t("cartEmptySubtitle")}
              continueShoppingLabel={t("navMenu")}
              clearCartLabel={t("clearCart")}
              subtotalLabel={t("subtotal")}
              totalLabel={t("total")}
              checkoutLabel={t("checkout")}
              removeLabel={t("remove")}
              decreaseLabel={t("decreaseQuantity")}
              increaseLabel={t("increaseQuantity")}
            />
          </div>
        </div>
      </header>

      {children}

      <footer className="mx-auto max-w-6xl border-t px-4 pb-10 pt-8 text-sm text-gray-600">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <span>© {new Date().getFullYear()} {t("brand")}</span>

          <div className="flex gap-6">
            <Link href={`/${locale}/impressum`} className="hover:underline">
              Impressum
            </Link>
            <Link href={`/${locale}/privacy`} className="hover:underline">
              Privacy / Datenschutz
            </Link>
          </div>
        </div>
      </footer>
    </NextIntlClientProvider>
  );
}