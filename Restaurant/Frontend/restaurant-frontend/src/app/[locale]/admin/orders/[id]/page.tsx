"use client";

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";

import {getAdminOrderById, updateOrderStatus} from "@/lib/admin-api";
import {formatPriceEUR} from "@/lib/http";
import {OrderStatus} from "@/types/order";
import type {Order} from "@/types/order";

import OrderStatusBadge from "@/components/admin/OrderStatusBadge";

const allowedTransitions: Record<OrderStatus, OrderStatus[]> = {
  NEW: ["PREPARING", "CANCELLED"],
  PREPARING: ["DONE", "CANCELLED"],
  DONE: [],
  CANCELLED: []
};

export default function AdminOrderDetailPage() {
  const {id} = useParams<{id: string}>();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        setLoading(true);
        const data = await getAdminOrderById(id);
        if (!cancelled) setOrder(data);
      } catch (error) {
        console.error(error);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [id]);

  async function handleStatusChange(nextStatus: OrderStatus) {
    if (!order) return;

    // ❌ nếu không hợp lệ → block
    if (!allowedTransitions[order.orderStatus].includes(nextStatus)) {
      return;
    }

    // ⚠️ confirm khi cancel
    if (nextStatus === "CANCELLED") {
      const ok = window.confirm("Are you sure you want to cancel this order?");
      if (!ok) return;
    }

    try {
      setUpdating(true);
      const updated = await updateOrderStatus(order.id, nextStatus);
      setOrder(updated);
    } catch (error) {
      console.error(error);
    } finally {
      setUpdating(false);
    }
  }

  if (loading) {
    return <main className="p-6">Loading...</main>;
  }

  if (!order) {
    return <main className="p-6">Order not found</main>;
  }

  const allowed = allowedTransitions[order.orderStatus];

  return (
    <main className="mx-auto max-w-4xl space-y-6 p-6">
      <h1 className="text-2xl font-extrabold">
        Order #{order.id.slice(0, 8)}
      </h1>

      {/* STATUS */}
      <div className="flex items-center justify-between rounded-2xl border p-4">
        <div>
          <div className="text-sm text-gray-500">Order status</div>
          <OrderStatusBadge status={order.orderStatus} />
        </div>

        <select
          value={order.orderStatus}
          disabled={updating}
          onChange={(e) =>
            handleStatusChange(e.target.value as OrderStatus)
          }
          className="rounded-xl border px-4 py-2 font-semibold"
        >
          <option value={order.orderStatus}>
            {order.orderStatus} (current)
          </option>

          {allowed.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      {/* ITEMS */}
      <div className="space-y-2">
        {order.items.map((item) => (
          <div key={item.productId} className="flex justify-between">
            <span>
              {item.productName} × {item.quantity}
            </span>
            <span>{formatPriceEUR(item.price)}</span>
          </div>
        ))}
      </div>

      <div className="text-lg font-bold">
        Total: {formatPriceEUR(order.totalPrice)}
      </div>
    </main>
  );
}