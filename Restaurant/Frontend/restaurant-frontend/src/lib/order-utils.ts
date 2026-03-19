import {PaymentStatus, OrderStatus} from "@/types/order";

export function getPaymentStatusColor(status: PaymentStatus) {
  switch (status) {
    case PaymentStatus.PAID:
      return "text-green-600 bg-green-50";

    case PaymentStatus.PENDING:
      return "text-yellow-600 bg-yellow-50";

    case PaymentStatus.FAILED:
    case PaymentStatus.CANCELED:
      return "text-red-600 bg-red-50";

    case PaymentStatus.REFUNDED:
      return "text-gray-600 bg-gray-100";

    default:
      return "text-gray-600 bg-gray-100";
  }
}