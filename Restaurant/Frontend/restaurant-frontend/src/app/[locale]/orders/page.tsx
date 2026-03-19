"use client";

import Link from "next/link";
import {useMemo} from "react";
import {useParams} from "next/navigation";
import {useTranslations} from "next-intl";

import RequireAuth from "@/components/RequireAuth";
import {useAuthStore} from "@/stores/auth-store";
import {getEmailFromToken} from "@/lib/jwt";
import {formatPriceEUR} from "@/lib/http";

type DemoOrder = {
  id: string;
  createdAt: string;
  totalPrice: number;
  paymentStatus: "PENDING" | "PAID" | "FAILED" | "CANCELED" | "REFUNDED";
  orderStatus: "NEW" | "PREPARING" | "DONE" | "CANCELLED";
  items: {
    name: string;
    quantity: number;
  }[];
};

function getPaymentBadgeClasses(status: DemoOrder["paymentStatus"]) {
  switch (status) {
    case "PAID":
      return "bg-green-50 text-green-700";
    case "PENDING":
      return "bg-yellow-50 text-yellow-700";
    case "FAILED":
    case "CANCELED":
      return "bg-red-50 text-red-700";
    case "REFUNDED":
      return "bg-gray-100 text-gray-700";
    default:
      return "bg-gray-100 text-gray-700";
  }
}

function getOrderBadgeClasses(status: DemoOrder["orderStatus"]) {
  switch (status) {
    case "DONE":
      return "bg-green-50 text-green-700";
    case "PREPARING":
      return "bg-blue-50 text-blue-700";
    case "NEW":
      return "bg-yellow-50 text-yellow-700";
    case "CANCELLED":
      return "bg-red-50 text-red-700";
    default:
      return "bg-gray-100 text-gray-700";
  }
}

export default function OrdersPage() {
  const {locale} = useParams<{locale: string}>();
  const t = useTranslations("orders");

  const accessToken = useAuthStore((state) => state.accessToken);
  const email = useMemo(() => getEmailFromToken(accessToken), [accessToken]);

  // TODO: replace with real backend orders API
  const orders: DemoOrder[] = [];

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

          {email ? (
            <div className="mt-4 text-sm font-semibold text-gray-900 break-all">
              {email}
            </div>
          ) : null}
        </section>

        {orders.length === 0 ? (
          <section className="rounded-3xl border bg-white p-10 text-center shadow-sm">
            <div className="text-5xl">📦</div>

            <h2 className="mt-4 text-2xl font-extrabold text-gray-900">
              {t("emptyTitle")}
            </h2>

            <p className="mt-2 text-gray-600">
              {t("emptySubtitle")}
            </p>

            <div className="mt-6 flex flex-col justify-center gap-3 sm:flex-row">
              <Link
                href={`/${locale}/menu`}
                className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
              >
                {t("goToMenu")}
              </Link>

              <Link
                href={`/${locale}/account`}
                className="inline-flex items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("goToAccount")}
              </Link>
            </div>
          </section>
        ) : (
          <section className="space-y-4">
            {orders.map((order) => (
              <article
                key={order.id}
                className="rounded-3xl border bg-white p-6 shadow-sm transition hover:shadow-md"
              >
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="min-w-0">
                    <div className="text-sm font-semibold text-gray-500">
                      {t("orderNumber")}
                    </div>

                    <h2 className="mt-1 text-xl font-extrabold text-gray-900 break-all">
                      #{order.id}
                    </h2>

                    <div className="mt-2 text-sm text-gray-600">
                      {t("placedAt")}: {order.createdAt}
                    </div>

                    <div className="mt-4 flex flex-wrap gap-2">
                      <span
                        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getPaymentBadgeClasses(
                          order.paymentStatus
                        )}`}
                      >
                        {t(`paymentStatusLabels.${order.paymentStatus}`)}
                      </span>

                      <span
                        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getOrderBadgeClasses(
                          order.orderStatus
                        )}`}
                      >
                        {t(`orderStatusLabels.${order.orderStatus}`)}
                      </span>
                    </div>
                  </div>

                  <div className="shrink-0 rounded-2xl bg-blue-50 px-4 py-3">
                    <div className="text-sm text-gray-500">{t("total")}</div>
                    <div className="text-lg font-extrabold text-blue-700">
                      {formatPriceEUR(order.totalPrice)}
                    </div>
                  </div>
                </div>

                <div className="mt-6 border-t pt-5">
                  <div className="text-sm font-semibold text-gray-700">
                    {t("items")}
                  </div>

                  <div className="mt-3 space-y-2">
                    {order.items.map((item, index) => (
                      <div
                        key={`${order.id}-${index}`}
                        className="flex items-center justify-between text-sm text-gray-700"
                      >
                        <span>
                          {item.name} × {item.quantity}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                  <Link
                    href={`/${locale}/order/${order.id}`}
                    className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50"
                  >
                    {t("viewOrder")}
                  </Link>

                  <Link
                    href={`/${locale}/menu`}
                    className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
                  >
                    {t("reorder")}
                  </Link>
                </div>
              </article>
            ))}
          </section>
        )}
      </main>
    </RequireAuth>
  );
}