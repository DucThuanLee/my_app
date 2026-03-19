"use client";

import Link from "next/link";
import {useParams} from "next/navigation";
import {useTranslations} from "next-intl";

import RequireAuth from "@/components/RequireAuth";
import {useAuthStore} from "@/stores/auth-store";

export default function AccountPage() {
  const {locale} = useParams<{locale: string}>();
  const t = useTranslations("account");

  const accessToken = useAuthStore((state) => state.accessToken);

  return (
    <RequireAuth>
      <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
        <section className="rounded-3xl border bg-white p-8 shadow-sm">
          <span className="inline-flex rounded-full bg-blue-50 px-4 py-1 text-sm font-semibold text-blue-700">
            {t("badge")}
          </span>

          <h1 className="mt-4 text-3xl font-extrabold text-gray-900">
            {t("title")}
          </h1>

          <p className="mt-2 text-gray-600">
            {t("subtitle")}
          </p>
        </section>

        <section className="grid gap-6 lg:grid-cols-2">
          <div className="rounded-3xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-extrabold text-gray-900">
              {t("sessionTitle")}
            </h2>

            <p className="mt-2 text-sm text-gray-600">
              {t("sessionSubtitle")}
            </p>

            <div className="mt-5 rounded-2xl bg-blue-50 p-4">
              <div className="text-sm font-semibold text-blue-700">
                {t("tokenLabel")}
              </div>

              <div className="mt-2 break-all text-sm text-gray-700">
                {accessToken ?? "-"}
              </div>
            </div>
          </div>

          <div className="rounded-3xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-extrabold text-gray-900">
              {t("quickActions")}
            </h2>

            <p className="mt-2 text-sm text-gray-600">
              {t("quickActionsSubtitle")}
            </p>

            <div className="mt-5 grid gap-3">
              <Link
                href={`/${locale}/menu`}
                className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("goToMenu")}
              </Link>

              <Link
                href={`/${locale}/cart`}
                className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("goToCart")}
              </Link>

              <Link
                href={`/${locale}/checkout`}
                className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
              >
                {t("goToCheckout")}
              </Link>
            </div>
          </div>
        </section>
      </main>
    </RequireAuth>
  );
}