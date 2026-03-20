"use client";

import Link from "next/link";
import {useEffect, useMemo, useState} from "react";
import {useParams} from "next/navigation";
import {useTranslations} from "next-intl";

import RequireAuth from "@/components/RequireAuth";
import OrderProgress from "@/components/OrderProgress";
import {useAuthStore} from "@/stores/auth-store";
import {getEmailFromToken} from "@/lib/jwt";
import {formatPriceEUR} from "@/lib/http";
import {getMyOrders} from "@/lib/order-api";
import {OrderStatus, PaymentStatus, type Order, type PageResponse} from "@/types/order";

function getPaymentBadgeClasses(status: PaymentStatus) {
  switch (status) {
    case PaymentStatus.PAID:
      return "bg-green-50 text-green-700";
    case PaymentStatus.PENDING:
      return "bg-yellow-50 text-yellow-700";
    case PaymentStatus.FAILED:
    case PaymentStatus.CANCELED:
      return "bg-red-50 text-red-700";
    case PaymentStatus.REFUNDED:
      return "bg-gray-100 text-gray-700";
    default:
      return "bg-gray-100 text-gray-700";
  }
}

function shouldPollOrders(orders: Order[]) {
  return orders.some(
    (order) =>
      order.paymentStatus === PaymentStatus.PENDING ||
      order.orderStatus === OrderStatus.NEW ||
      order.orderStatus === OrderStatus.PREPARING
  );
}

export default function OrdersPage() {
  const {locale} = useParams<{locale: string}>();
  const t = useTranslations("orders");

  const accessToken = useAuthStore((state) => state.accessToken);
  const email = useMemo(() => getEmailFromToken(accessToken), [accessToken]);

  const [pageData, setPageData] = useState<PageResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [statusFilter, setStatusFilter] = useState<OrderStatus | "">("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");
  const [page, setPage] = useState(0);
  const size = 10;

  useEffect(() => {
    let cancelled = false;

    async function loadOrders(isBackground = false) {
      try {
        if (isBackground) {
          setRefreshing(true);
        } else {
          setLoading(true);
        }

        setErrorMessage("");

        const data = await getMyOrders({
          page,
          size,
          status: statusFilter,
          sortBy,
          sortDir
        });

        if (!cancelled) {
          setPageData(data);
        }
      } catch (error) {
        console.error(error);

        if (!cancelled) {
          setErrorMessage(t("loadError"));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
          setRefreshing(false);
        }
      }
    }

    loadOrders(false);

    return () => {
      cancelled = true;
    };
  }, [page, size, statusFilter, sortBy, sortDir, t]);

  useEffect(() => {
    const orders = pageData?.content ?? [];
    if (!shouldPollOrders(orders)) return;

    let cancelled = false;

    const interval = window.setInterval(async () => {
      try {
        setRefreshing(true);

        const data = await getMyOrders({
          page,
          size,
          status: statusFilter,
          sortBy,
          sortDir
        });

        if (!cancelled) {
          setPageData(data);
        }
      } catch (error) {
        console.error("Orders polling failed:", error);
      } finally {
        if (!cancelled) {
          setRefreshing(false);
        }
      }
    }, 5000);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [pageData, page, size, statusFilter, sortBy, sortDir]);

  const orders = pageData?.content ?? [];

  function handleStatusChange(value: string) {
    setPage(0);
    setStatusFilter(value as OrderStatus | "");
  }

  function handleSortByChange(value: string) {
    setPage(0);
    setSortBy(value);
  }

  function handleSortDirChange(value: string) {
    setPage(0);
    setSortDir(value as "asc" | "desc");
  }

  return (
    <RequireAuth>
      <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
        <section className="rounded-3xl border bg-white p-8 shadow-sm">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
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
                <div className="mt-4 break-all text-sm font-semibold text-gray-900">
                  {email}
                </div>
              ) : null}
            </div>

            {refreshing ? (
              <div className="inline-flex rounded-full bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700">
                {t("refreshing")}
              </div>
            ) : null}
          </div>

          <div className="mt-6 grid gap-3 md:grid-cols-4">
            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-700">
                {t("filterStatus")}
              </label>
              <select
                value={statusFilter}
                onChange={(e) => handleStatusChange(e.target.value)}
                className="w-full rounded-2xl border px-4 py-3"
              >
                <option value="">{t("allStatuses")}</option>
                <option value="NEW">NEW</option>
                <option value="PREPARING">PREPARING</option>
                <option value="DONE">DONE</option>
                <option value="CANCELLED">CANCELLED</option>
                <option value="DELIVERED">DELIVERED</option>
              </select>
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-700">
                {t("sortBy")}
              </label>
              <select
                value={sortBy}
                onChange={(e) => handleSortByChange(e.target.value)}
                className="w-full rounded-2xl border px-4 py-3"
              >
                <option value="createdAt">{t("sortCreatedAt")}</option>
                <option value="totalPrice">{t("sortTotalPrice")}</option>
              </select>
            </div>

            <div>
              <label className="mb-2 block text-sm font-semibold text-gray-700">
                {t("sortDirection")}
              </label>
              <select
                value={sortDir}
                onChange={(e) => handleSortDirChange(e.target.value)}
                className="w-full rounded-2xl border px-4 py-3"
              >
                <option value="desc">{t("sortDesc")}</option>
                <option value="asc">{t("sortAsc")}</option>
              </select>
            </div>

            <div className="flex items-end">
              <div className="rounded-2xl bg-blue-50 px-4 py-3 text-sm font-semibold text-blue-700">
                {t("resultsCount", {
                  count: pageData?.totalElements ?? 0
                })}
              </div>
            </div>
          </div>
        </section>

        {loading ? (
          <section className="space-y-4">
            {Array.from({length: 3}).map((_, index) => (
              <div
                key={index}
                className="rounded-3xl border bg-white p-6 shadow-sm"
              >
                <div className="h-6 w-40 animate-pulse rounded bg-gray-200" />
                <div className="mt-3 h-4 w-52 animate-pulse rounded bg-gray-100" />
                <div className="mt-6 h-20 animate-pulse rounded-2xl bg-gray-100" />
              </div>
            ))}
          </section>
        ) : errorMessage ? (
          <section className="rounded-3xl border border-red-200 bg-white p-10 text-center shadow-sm">
            <div className="text-5xl">⚠️</div>
            <h2 className="mt-4 text-2xl font-extrabold text-gray-900">
              {t("errorTitle")}
            </h2>
            <p className="mt-2 text-gray-600">{errorMessage}</p>
            <Link
              href={`/${locale}/account`}
              className="mt-6 inline-flex items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
            >
              {t("goToAccount")}
            </Link>
          </section>
        ) : orders.length === 0 ? (
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
          <>
            <section className="space-y-4">
              {orders.map((order) => (
                <article
                  key={order.id}
                  className="rounded-3xl border bg-white p-6 shadow-sm transition hover:shadow-md"
                >
                  <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-semibold text-gray-500">
                        {t("orderNumber")}
                      </div>

                      <h2 className="mt-1 break-all text-xl font-extrabold text-gray-900">
                        #{order.id}
                      </h2>

                      <div className="mt-2 text-sm text-gray-600">
                        {t("placedAt")}: {new Date(order.createdAt).toLocaleString(locale)}
                      </div>

                      <div className="mt-4 flex flex-wrap gap-2">
                        <span
                          className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getPaymentBadgeClasses(
                            order.paymentStatus
                          )}`}
                        >
                          {t(`paymentStatusLabels.${order.paymentStatus}`)}
                        </span>

                        <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
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

                  <div className="mt-6">
                    <OrderProgress
                      status={order.orderStatus}
                      newLabel={t("progressNew")}
                      preparingLabel={t("progressPreparing")}
                      doneLabel={t("progressDone")}
                    />
                  </div>

                  <div className="mt-6 border-t pt-5">
                    <div className="text-sm font-semibold text-gray-700">
                      {t("items")}
                    </div>

                    <div className="mt-3 space-y-2">
                      {order.items.map((item) => (
                        <div
                          key={`${order.id}-${item.productId}`}
                          className="flex items-center justify-between text-sm text-gray-700"
                        >
                          <span>
                            {item.productName} × {item.quantity}
                          </span>
                          <span>{formatPriceEUR(item.price * item.quantity)}</span>
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

            <section className="flex items-center justify-between rounded-3xl border bg-white px-6 py-4 shadow-sm">
              <button
                type="button"
                disabled={pageData?.first}
                onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                className="rounded-2xl border px-4 py-2 font-semibold text-blue-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t("previousPage")}
              </button>

              <div className="text-sm font-semibold text-gray-700">
                {t("pageIndicator", {
                  page: (pageData?.number ?? 0) + 1,
                  totalPages: pageData?.totalPages ?? 1
                })}
              </div>

              <button
                type="button"
                disabled={pageData?.last}
                onClick={() => setPage((prev) => prev + 1)}
                className="rounded-2xl border px-4 py-2 font-semibold text-blue-700 hover:bg-blue-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {t("nextPage")}
              </button>
            </section>
          </>
        )}
      </main>
    </RequireAuth>
  );
}