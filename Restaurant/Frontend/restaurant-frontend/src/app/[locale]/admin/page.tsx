"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { getAdminOrders } from "@/lib/admin-api";
import { formatPriceEUR } from "@/lib/http";
import { OrderStatus, PaymentStatus, type Order, type PageResponse } from "@/types/order";

import AdminStatCard from "@/components/admin/AdminStatCard";
import AdminRevenueChart from "@/components/admin/AdminRevenueChart";
import OrderStatusBadge from "@/components/admin/OrderStatusBadge";
import PaymentBadge from "@/components/admin/PaymentBadge";
import RefundButton from "@/components/admin/RefundButton";
import { useParams } from "next/navigation";

function buildRevenueData(orders: Order[]) {
  const now = new Date();
  const days: string[] = [];

  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    days.push(d.toISOString().slice(0, 10));
  }

  const map = new Map<string, number>(days.map((day) => [day, 0]));

  for (const order of orders) {
    if (order.paymentStatus !== PaymentStatus.PAID) continue;

    const key = new Date(order.createdAt).toISOString().slice(0, 10);
    if (map.has(key)) {
      map.set(key, (map.get(key) ?? 0) + order.totalPrice);
    }
  }

  return Array.from(map.entries()).map(([date, revenue]) => ({
    date: date.slice(5),
    revenue: Number(revenue.toFixed(2))
  }));
}

export default function AdminDashboardPage() {
  const { locale } = useParams<{ locale: string }>();
  const [data, setData] = useState<PageResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        setErrorMessage("");

        const result = await getAdminOrders({
          page: 0,
          size: 50,
          sortBy: "createdAt",
          sortDir: "desc"
        });

        if (!cancelled) {
          setData(result);
        }
      } catch (error) {
        console.error(error);
        if (!cancelled) {
          setErrorMessage("Could not load admin dashboard.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, []);

  const orders = data?.content ?? [];

  const totalOrders = orders.length;
  const paidOrders = orders.filter((o) => o.paymentStatus === PaymentStatus.PAID).length;
  const pendingOrders = orders.filter((o) => o.orderStatus === OrderStatus.NEW).length;
  const preparingOrders = orders.filter((o) => o.orderStatus === OrderStatus.PREPARING).length;

  const totalRevenue = orders
    .filter((o) => o.paymentStatus === PaymentStatus.PAID)
    .reduce((sum, order) => sum + order.totalPrice, 0);

  const averageOrderValue =
    paidOrders > 0 ? totalRevenue / paidOrders : 0;

  const revenueData = useMemo(() => buildRevenueData(orders), [orders]);

  return (
    <main className="mx-auto max-w-7xl space-y-8 p-6">
      <section className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <div className="inline-flex rounded-full bg-blue-50 px-4 py-1 text-sm font-semibold text-blue-700">
            Admin
          </div>
          <h1 className="mt-4 text-3xl font-extrabold text-gray-900">
            Dashboard
          </h1>
          <p className="mt-2 text-gray-600">
            Orders, revenue and operational status overview.
          </p>
        </div>

        <Link
          href={`/${locale}/admin/orders`}
          className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
        >
          Open orders manager
        </Link>
      </section>

      {loading ? (
        <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="rounded-3xl border bg-white p-6 shadow-sm">
              <div className="h-4 w-24 animate-pulse rounded bg-gray-200" />
              <div className="mt-4 h-8 w-28 animate-pulse rounded bg-gray-100" />
              <div className="mt-3 h-4 w-40 animate-pulse rounded bg-gray-100" />
            </div>
          ))}
        </section>
      ) : errorMessage ? (
        <section className="rounded-3xl border border-red-200 bg-white p-8 shadow-sm">
          <div className="text-lg font-bold text-gray-900">Dashboard error</div>
          <p className="mt-2 text-gray-600">{errorMessage}</p>
        </section>
      ) : (
        <>
          <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
            <AdminStatCard
              title="Orders loaded"
              value={String(totalOrders)}
              subtitle="From the latest admin query result."
            />
            <AdminStatCard
              title="Paid revenue"
              value={formatPriceEUR(totalRevenue)}
              subtitle="Sum of paid orders."
            />
            <AdminStatCard
              title="Pending orders"
              value={String(pendingOrders)}
              subtitle="Orders still marked as NEW."
            />
            <AdminStatCard
              title="Average order"
              value={formatPriceEUR(averageOrderValue)}
              subtitle="Average paid order value."
            />
          </section>

          <section className="grid gap-6 xl:grid-cols-[1.5fr_1fr]">
            <AdminRevenueChart data={revenueData} />

            <div className="rounded-3xl border bg-white p-6 shadow-sm">
              <h2 className="text-xl font-extrabold text-gray-900">
                Operational status
              </h2>

              <div className="mt-6 space-y-4">
                <div className="flex items-center justify-between rounded-2xl bg-gray-50 px-4 py-3">
                  <span className="text-sm font-semibold text-gray-700">New</span>
                  <span className="text-lg font-extrabold text-gray-900">
                    {pendingOrders}
                  </span>
                </div>

                <div className="flex items-center justify-between rounded-2xl bg-yellow-50 px-4 py-3">
                  <span className="text-sm font-semibold text-yellow-800">Preparing</span>
                  <span className="text-lg font-extrabold text-yellow-900">
                    {preparingOrders}
                  </span>
                </div>

                <div className="flex items-center justify-between rounded-2xl bg-green-50 px-4 py-3">
                  <span className="text-sm font-semibold text-green-800">Paid</span>
                  <span className="text-lg font-extrabold text-green-900">
                    {paidOrders}
                  </span>
                </div>

                <div className="flex items-center justify-between rounded-2xl bg-red-50 px-4 py-3">
                  <span className="text-sm font-semibold text-red-800">Cancelled</span>
                  <span className="text-lg font-extrabold text-red-900">
                    {orders.filter((o) => o.orderStatus === OrderStatus.CANCELLED).length}
                  </span>
                </div>
              </div>
            </div>
          </section>

          <section className="rounded-3xl border bg-white p-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-extrabold text-gray-900">
                Recent orders
              </h2>

              <Link
                href={`/${locale}/admin/orders`}
                className="text-sm font-semibold text-blue-700 hover:underline"
              >
                View all
              </Link>
            </div>

            <div className="mt-6 overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b bg-gray-50 text-left text-gray-600">
                  <tr>
                    <th className="px-4 py-3">Order</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Payment</th>
                    <th className="px-4 py-3">Total</th>
                    <th className="px-4 py-3">Refunded</th>
                    <th className="px-4 py-3">Created</th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {orders.slice(0, 8).map((order) => {
                    const refunded = order.refundedAmount ?? 0;
                    const remaining = Math.max(0, order.totalPrice - refunded);

                    return (
                      <tr key={order.id} className="border-b last:border-b-0 hover:bg-gray-50">
                        <td className="px-4 py-4 font-semibold text-gray-900">
                          #{order.id.slice(0, 8)}
                        </td>

                        <td className="px-4 py-4">
                          <OrderStatusBadge status={order.orderStatus} />
                        </td>

                        <td className="px-4 py-4">
                          <PaymentBadge status={order.paymentStatus} />
                        </td>

                        <td className="px-4 py-4 font-semibold">
                          {formatPriceEUR(order.totalPrice)}
                        </td>

                        {/* ✅ REFUNDED FIX */}
                        <td className="px-4 py-4">
                          {refunded > 0 ? (
                            <div className="flex flex-col">
                              <span className="font-semibold text-red-600">
                                -{formatPriceEUR(refunded)}
                              </span>

                              {remaining > 0 && (
                                <span className="text-xs text-gray-500">
                                  Remaining: {formatPriceEUR(remaining)}
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="text-gray-400">—</span>
                          )}
                        </td>

                        <td className="px-4 py-4 text-gray-500">
                          {new Date(order.createdAt).toLocaleString()}
                        </td>

                        <td className="px-4 py-4">
                          <div className="flex flex-col gap-2">
                            <Link
                              href={`/${locale}/admin/orders/${order.id}`}
                              className="font-semibold text-blue-700 hover:underline"
                            >
                              Open
                            </Link>

                            {/* ✅ REFUND FIX */}
                            <RefundButton
                              orderId={order.id}
                              total={order.totalPrice}
                              refundedAmount={refunded}
                              paymentStatus={order.paymentStatus}
                              disabled={
                                order.paymentStatus !== PaymentStatus.PAID ||
                                remaining <= 0
                              }
                              onSuccess={() => window.location.reload()}
                            />
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </main>
  );
}