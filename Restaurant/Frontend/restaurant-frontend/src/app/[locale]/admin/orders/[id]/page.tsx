"use client";

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";

import {getOrderById} from "@/lib/order-api";
import {updateOrderStatus} from "@/lib/admin-api";
import {formatPriceEUR} from "@/lib/http";
import {OrderStatus} from "@/types/order";
import type {Order} from "@/types/order";

export default function AdminOrderDetailPage() {
  const {id} = useParams<{id: string}>();

  const [order, setOrder] = useState<Order | null>(null);

  useEffect(() => {
    getOrderById(id).then(setOrder);
  }, [id]);

  if (!order) return <div>Loading...</div>;

  async function update(status: OrderStatus) {
    const updated = await updateOrderStatus(order.id, status);
    setOrder(updated);
  }

  return (
    <main className="mx-auto max-w-4xl p-6 space-y-6">
      <h1 className="text-2xl font-bold">Order #{order.id}</h1>

      <div>Status: {order.orderStatus}</div>
      <div>Payment: {order.paymentStatus}</div>

      <div className="flex gap-2">
        <button onClick={() => update(OrderStatus.PREPARING)}>
          PREPARING
        </button>

        <button onClick={() => update(OrderStatus.DONE)}>
          DONE
        </button>
      </div>

      <div className="space-y-2">
        {order.items.map((i) => (
          <div key={i.productId}>
            {i.productName} × {i.quantity} — {formatPriceEUR(i.price)}
          </div>
        ))}
      </div>

      <div className="text-lg font-bold">
        Total: {formatPriceEUR(order.totalPrice)}
      </div>
    </main>
  );
}