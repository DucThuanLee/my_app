"use client";

import {useEffect, useMemo, useState} from "react";
import {useTranslations} from "next-intl";
import {getStripePaymentStatus} from "@/lib/payment-api";
import {getOrderStatusClasses, getPaymentStatusClasses} from "@/lib/order-status";
import {OrderStatus, PaymentStatus} from "@/types/order";
import type {PaymentStatusResponse} from "@/types/payment";
import {useCartStore} from "@/stores/cart-store";

type Props = {
  orderId: string;
  initialStatus: PaymentStatusResponse;
};

function normalizePaymentStatus(status: string): PaymentStatus {
  if (Object.values(PaymentStatus).includes(status as PaymentStatus)) {
    return status as PaymentStatus;
  }
  return PaymentStatus.PENDING;
}

function normalizeOrderStatus(status: string): OrderStatus {
  if (Object.values(OrderStatus).includes(status as OrderStatus)) {
    return status as OrderStatus;
  }
  return OrderStatus.NEW;
}

export default function OrderStatusLive({orderId, initialStatus}: Props) {
  const t = useTranslations("order");

  const [status, setStatus] = useState(initialStatus);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const markOrderCartCleared = useCartStore((state) => state.markOrderCartCleared);
  const lastClearedOrderId = useCartStore((state) => state.lastClearedOrderId);

  const paymentStatus = useMemo(
    () => normalizePaymentStatus(status.paymentStatus),
    [status.paymentStatus]
  );

  const orderStatus = useMemo(
    () => normalizeOrderStatus(status.orderStatus),
    [status.orderStatus]
  );

  useEffect(() => {
    if (paymentStatus === PaymentStatus.PAID && lastClearedOrderId !== orderId) {
      markOrderCartCleared(orderId);
    }
  }, [paymentStatus, lastClearedOrderId, orderId, markOrderCartCleared]);

  useEffect(() => {
    const shouldPoll = paymentStatus === PaymentStatus.PENDING;
    if (!shouldPoll) return;

    let cancelled = false;

    const interval = window.setInterval(async () => {
      try {
        setIsRefreshing(true);
        const next = await getStripePaymentStatus(orderId);

        if (!cancelled) {
          setStatus(next);
        }

        const nextPaymentStatus = normalizePaymentStatus(next.paymentStatus);
        if (nextPaymentStatus !== PaymentStatus.PENDING) {
          window.clearInterval(interval);
        }
      } catch (error) {
        console.error("Polling payment status failed:", error);
      } finally {
        if (!cancelled) {
          setIsRefreshing(false);
        }
      }
    }, 3000);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [orderId, paymentStatus]);

  const paymentStatusLabel = t(`statusLabels.payment.${paymentStatus}`);
  const orderStatusLabel = t(`statusLabels.order.${orderStatus}`);

  return (
    <div className="mt-6 flex flex-wrap gap-3">
      <span
        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getPaymentStatusClasses(
          paymentStatus
        )}`}
      >
        {t("paymentStatus")}: {paymentStatusLabel}
      </span>

      <span
        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getOrderStatusClasses(
          orderStatus
        )}`}
      >
        {t("orderStatus")}: {orderStatusLabel}
      </span>

      {paymentStatus === PaymentStatus.PENDING ? (
        <span className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
          {isRefreshing ? t("checkingPayment") : t("waitingForPayment")}
        </span>
      ) : null}

      {paymentStatus === PaymentStatus.PAID ? (
        <span className="inline-flex rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
          {t("cartCleared")}
        </span>
      ) : null}
    </div>
  );
}