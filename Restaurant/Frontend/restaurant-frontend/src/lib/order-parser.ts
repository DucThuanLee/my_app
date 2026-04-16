import type {PageResponse} from "@/types/order";
import {
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
  type Order,
  type OrderItem
} from "@/types/order";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

export function isPaymentStatus(value: unknown): value is PaymentStatus {
  return Object.values(PaymentStatus).includes(value as PaymentStatus);
}

export function isPaymentMethod(value: unknown): value is PaymentMethod {
  return Object.values(PaymentMethod).includes(value as PaymentMethod);
}

export function isOrderStatus(value: unknown): value is OrderStatus {
  return Object.values(OrderStatus).includes(value as OrderStatus);
}

export function parseOrderItem(value: unknown): OrderItem {
  if (!isRecord(value)) {
    throw new Error("Invalid order item: expected object");
  }

  const {productId, productName, quantity, price} = value;

  if (typeof productId !== "string" || productId.trim() === "") {
    throw new Error("Invalid order item: productId must be a non-empty string");
  }

  if (typeof productName !== "string" || productName.trim() === "") {
    throw new Error("Invalid order item: productName must be a non-empty string");
  }

  if (typeof quantity !== "number" || !Number.isFinite(quantity)) {
    throw new Error("Invalid order item: quantity must be a number");
  }

  if (typeof price !== "number" || !Number.isFinite(price)) {
    throw new Error("Invalid order item: price must be a number");
  }

  return {
    productId,
    productName,
    quantity,
    price
  };
}

export function parseOrder(value: unknown): Order {
  if (!isRecord(value)) {
    throw new Error("Invalid order: expected object");
  }

  const {
    id,
    totalPrice,
    refundedAmount, // 👈 thêm
    paymentMethod,
    paymentStatus,
    orderStatus,
    createdAt,
    items
  } = value;

  if (typeof id !== "string" || id.trim() === "") {
    throw new Error("Invalid order: id must be a non-empty string");
  }

  if (typeof totalPrice !== "number" || !Number.isFinite(totalPrice)) {
    throw new Error("Invalid order: totalPrice must be a number");
  }

  // ✅ validate refundedAmount
  let safeRefunded = 0;

  if (refundedAmount != null) {
    if (typeof refundedAmount !== "number" || !Number.isFinite(refundedAmount)) {
      throw new Error("Invalid order: refundedAmount must be a number");
    }

    safeRefunded = refundedAmount;
  }

  if (!isPaymentMethod(paymentMethod)) {
    throw new Error(`Invalid order: unknown paymentMethod "${String(paymentMethod)}"`);
  }

  if (!isPaymentStatus(paymentStatus)) {
    throw new Error(`Invalid order: unknown paymentStatus "${String(paymentStatus)}"`);
  }

  if (!isOrderStatus(orderStatus)) {
    throw new Error(`Invalid order: unknown orderStatus "${String(orderStatus)}"`);
  }

  if (typeof createdAt !== "string" || createdAt.trim() === "") {
    throw new Error("Invalid order: createdAt must be a non-empty string");
  }

  if (!Array.isArray(items)) {
    throw new Error("Invalid order: items must be an array");
  }

  return {
    id,
    totalPrice,
    refundedAmount: safeRefunded, 
    paymentMethod,
    paymentStatus,
    orderStatus,
    createdAt,
    items: items.map(parseOrderItem)
  };
}

export function parseOrderPage(value: unknown): PageResponse<Order> {
  if (!isRecord(value)) {
    throw new Error("Invalid order page: expected object");
  }

  const {
    content,
    number,
    size,
    totalElements,
    totalPages,
    first,
    last
  } = value;

  if (!Array.isArray(content)) {
    throw new Error("Invalid order page: content must be an array");
  }

  if (typeof number !== "number") {
    throw new Error("Invalid order page: number must be a number");
  }

  if (typeof size !== "number") {
    throw new Error("Invalid order page: size must be a number");
  }

  if (typeof totalElements !== "number") {
    throw new Error("Invalid order page: totalElements must be a number");
  }

  if (typeof totalPages !== "number") {
    throw new Error("Invalid order page: totalPages must be a number");
  }

  if (typeof first !== "boolean") {
    throw new Error("Invalid order page: first must be a boolean");
  }

  if (typeof last !== "boolean") {
    throw new Error("Invalid order page: last must be a boolean");
  }

  return {
    content: content.map(parseOrder),
    number,
    size,
    totalElements,
    totalPages,
    first,
    last
  };
}