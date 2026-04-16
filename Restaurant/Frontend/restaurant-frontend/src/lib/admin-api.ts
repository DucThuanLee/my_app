import {backendUrl, buildAuthHeaders, readJsonOrThrow} from "@/lib/http";
import {parseOrder, parseOrderPage} from "@/lib/order-parser";
import type {Order, OrderStatus, PageResponse} from "@/types/order";

type AdminOrderParams = {
  page?: number;
  size?: number;
  status?: OrderStatus | "";
  sortBy?: string;
  sortDir?: "asc" | "desc";
};

export async function getAdminOrders(
  params: AdminOrderParams = {}
): Promise<PageResponse<Order>> {
  const url = backendUrl("/api/admin/orders");

  url.searchParams.set("page", String(params.page ?? 0));
  url.searchParams.set("size", String(params.size ?? 50));
  url.searchParams.set("sortBy", params.sortBy ?? "createdAt");
  url.searchParams.set("sortDir", params.sortDir ?? "desc");

  if (params.status) {
    url.searchParams.set("status", params.status);
  }

  const res = await fetch(url.toString(), {
    headers: buildAuthHeaders(),
    cache: "no-store"
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to fetch admin orders");
  return parseOrderPage(json);
}

export async function updateOrderStatus(
  orderId: string,
  status: OrderStatus
): Promise<Order> {
  const url = backendUrl(`/api/admin/orders/${orderId}/status`);

  const res = await fetch(url.toString(), {
    method: "PATCH",
    headers: {
      ...buildAuthHeaders(),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({status})
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to update order status");
  return parseOrder(json);
}

export async function getAdminOrderById(id: string): Promise<Order> {
  const url = backendUrl(`/api/admin/orders/${id}`);

  const res = await fetch(url.toString(), {
    headers: buildAuthHeaders(),
    cache: "no-store"
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to fetch admin order");
  return parseOrder(json);
}

export async function deleteOrder(orderId: string): Promise<void> {
  const url = backendUrl(`/api/admin/orders/${orderId}`);

  const res = await fetch(url.toString(), {
    method: "DELETE",
    headers: buildAuthHeaders()
  });

  if (!res.ok) {
    throw new Error(`Failed to delete order: ${res.status} ${res.statusText}`);
  }
}

// Refund order
export type CreateRefundRequest = {
  orderId: string;
  amount?: number; // optional → null = full refund
  reason?: string;
};

export async function refundOrder(payload: CreateRefundRequest): Promise<void> {
  const url = backendUrl("/api/admin/payments/stripe/refunds");

  const res = await fetch(url.toString(), {
    method: "POST",
    headers: {
      ...buildAuthHeaders(),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Refund failed: ${text}`);
  }
}