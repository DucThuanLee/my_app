import type {CreateOrderRequest, Order, OrderStatus, PageResponse} from "@/types/order";
import {backendUrl, buildAuthHeaders, readJsonOrThrow} from "@/lib/http";
import {parseOrder, parseOrderPage} from "@/lib/order-parser";

export async function createOrder(payload: CreateOrderRequest): Promise<Order> {
  const url = backendUrl("/api/orders");

  const res = await fetch(url.toString(), {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to create order");
  return parseOrder(json);
}

export async function getOrderById(id: string): Promise<Order> {
  const url = backendUrl(`/api/orders/${id}`);

  const res = await fetch(url.toString(), {
    cache: "no-store"
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to fetch order");
  return parseOrder(json);
}

type GetMyOrdersParams = {
  page?: number;
  size?: number;
  status?: OrderStatus | "";
  sortBy?: string;
  sortDir?: "asc" | "desc";
};

export async function getMyOrders(
  params: GetMyOrdersParams = {}
): Promise<PageResponse<Order>> {
  const url = backendUrl("/api/orders/me");

  url.searchParams.set("page", String(params.page ?? 0));
  url.searchParams.set("size", String(params.size ?? 10));

  if (params.status) {
    url.searchParams.set("status", params.status);
  }

  if (params.sortBy) {
    url.searchParams.set("sortBy", params.sortBy);
  }

  if (params.sortDir) {
    url.searchParams.set("sortDir", params.sortDir);
  }

  const res = await fetch(url.toString(), {
    method: "GET",
    headers: buildAuthHeaders(),
    cache: "no-store"
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to fetch my orders");
  return parseOrderPage(json);
}