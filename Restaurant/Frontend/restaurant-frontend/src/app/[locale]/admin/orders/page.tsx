"use client";

import {useEffect, useState} from "react";
import Link from "next/link";

import {getAdminOrders} from "@/lib/admin-api";
import {formatPriceEUR} from "@/lib/http";
import {OrderStatus} from "@/types/order";
import type {Order, PageResponse} from "@/types/order";

import OrderStatusBadge from "@/components/admin/OrderStatusBadge";
import PaymentBadge from "@/components/admin/PaymentBadge";

export default function AdminOrdersPage() {
  const [data, setData] = useState<PageResponse<Order> | null>(null);
  const [loading, setLoading] = useState(true);

  const [status, setStatus] = useState<OrderStatus | "">("");
  const [page, setPage] = useState(0);

  useEffect(() => {
    setLoading(true);

    getAdminOrders({page, status})
      .then(setData)
      .finally(() => setLoading(false));
  }, [page, status]);

  return (
    <main className="mx-auto max-w-7xl p-6 space-y-6">
      {/* HEADER */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-extrabold">Orders</h1>

        <select
          value={status}
          onChange={(e) => {
            setPage(0);
            setStatus(e.target.value as any);
          }}
          className="rounded-xl border px-3 py-2"
        >
          <option value="">All</option>
          <option value="NEW">NEW</option>
          <option value="PREPARING">PREPARING</option>
          <option value="DONE">DONE</option>
          <option value="CANCELLED">CANCELLED</option>
        </select>
      </div>

      {/* TABLE */}
      <div className="overflow-hidden rounded-2xl border bg-white shadow-sm">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-gray-600">
            <tr>
              <th className="px-4 py-3">Order</th>
              <th>Status</th>
              <th>Payment</th>
              <th>Total</th>
              <th>Date</th>
              <th></th>
            </tr>
          </thead>

          <tbody>
            {loading && (
              <tr>
                <td colSpan={6} className="p-6 text-center">
                  Loading...
                </td>
              </tr>
            )}

            {data?.content.map((o) => (
              <tr key={o.id} className="border-t hover:bg-gray-50">
                <td className="px-4 py-3 font-medium">
                  #{o.id.slice(0, 8)}
                </td>

                <td>
                  <OrderStatusBadge status={o.orderStatus} />
                </td>

                <td>
                  <PaymentBadge status={o.paymentStatus} />
                </td>

                <td className="font-semibold">
                  {formatPriceEUR(o.totalPrice)}
                </td>

                <td className="text-gray-500">
                  {new Date(o.createdAt).toLocaleString()}
                </td>

                <td>
                  <Link
                    href={`/en/admin/orders/${o.id}`}
                    className="text-blue-600 font-semibold hover:underline"
                  >
                    View →
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* PAGINATION */}
      <div className="flex items-center justify-between">
        <button
          disabled={data?.first}
          onClick={() => setPage((p) => p - 1)}
          className="rounded-xl border px-4 py-2 disabled:opacity-40"
        >
          ← Prev
        </button>

        <span className="text-sm text-gray-600">
          Page {data?.number + 1} / {data?.totalPages}
        </span>

        <button
          disabled={data?.last}
          onClick={() => setPage((p) => p + 1)}
          className="rounded-xl border px-4 py-2 disabled:opacity-40"
        >
          Next →
        </button>
      </div>
    </main>
  );
}