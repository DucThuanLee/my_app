import type {CreateOrderRequest, Order} from "@/types/order";
import {backendUrl, readJsonOrThrow} from "@/lib/http";
import {parseOrder} from "@/lib/order-parser";

/**
 * Create a new order in the Java backend.
 */
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

/**
 * Fetch one order by id.
 */
export async function getOrderById(id: string): Promise<Order> {
  const url = backendUrl(`/api/orders/${id}`);

  const res = await fetch(url.toString(), {
    cache: "no-store"
  });

  const json = await readJsonOrThrow<unknown>(res, "Failed to fetch order");
  return parseOrder(json);
}