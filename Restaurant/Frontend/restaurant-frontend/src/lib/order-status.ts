import {OrderStatus, PaymentStatus} from "@/types/order";

export function getPaymentStatusClasses(status: PaymentStatus) {
  switch (status) {
    case PaymentStatus.PAID:
      return "bg-green-50 text-green-700";

    case PaymentStatus.PENDING:
      return "bg-yellow-50 text-yellow-700";

    case PaymentStatus.FAILED:
    case PaymentStatus.CANCELED:
      return "bg-red-50 text-red-700";

    case PaymentStatus.REFUNDED:
      return "bg-gray-100 text-gray-700";

    default:
      return "bg-gray-100 text-gray-700";
  }
}

export function getOrderStatusClasses(status: OrderStatus) {
  switch (status) {
    case OrderStatus.DONE:
      return "bg-green-50 text-green-700";

    case OrderStatus.PREPARING:
      return "bg-blue-50 text-blue-700";

    case OrderStatus.NEW:
      return "bg-yellow-50 text-yellow-700";

    case OrderStatus.CANCELLED:
      return "bg-red-50 text-red-700";

    default:
      return "bg-gray-100 text-gray-700";
  }
}