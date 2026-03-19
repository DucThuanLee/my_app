"use client";

import {useTranslations} from "next-intl";
import RequireAuth from "@/components/RequireAuth";

export default function OrdersPage() {
  const t = useTranslations("orders");

  return (
    <RequireAuth>
      <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
        <section className="rounded-3xl border bg-white p-8 shadow-sm">
          <h1 className="text-3xl font-extrabold text-gray-900">
            {t("title")}
          </h1>
          <p className="mt-2 text-gray-600">
            {t("subtitle")}
          </p>
        </section>
      </main>
    </RequireAuth>
  );
}