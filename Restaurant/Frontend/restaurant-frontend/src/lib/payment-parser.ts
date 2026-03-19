import type {PaymentStatusResponse} from "@/types/payment";
import {OrderStatus, PaymentStatus} from "@/types/order";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function isPaymentStatus(value: unknown): value is PaymentStatus {
  return Object.values(PaymentStatus).includes(value as PaymentStatus);
}

function isOrderStatus(value: unknown): value is OrderStatus {
  return Object.values(OrderStatus).includes(value as OrderStatus);
}

export function parsePaymentStatusResponse(value: unknown): PaymentStatusResponse {
  if (!isRecord(value)) {
    throw new Error("Invalid payment status response");
  }

  const {orderId, stripePaymentIntentId, paymentStatus, orderStatus} = value;

  if (typeof orderId !== "string" || orderId.trim() === "") {
    throw new Error("Invalid payment status response: orderId");
  }

  if (typeof stripePaymentIntentId !== "string") {
    throw new Error("Invalid payment status response: stripePaymentIntentId");
  }

  if (!isPaymentStatus(paymentStatus)) {
    throw new Error(`Invalid payment status "${String(paymentStatus)}"`);
  }

  if (!isOrderStatus(orderStatus)) {
    throw new Error(`Invalid order status "${String(orderStatus)}"`);
  }

  return {
    orderId,
    stripePaymentIntentId,
    paymentStatus,
    orderStatus
  };
}