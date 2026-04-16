import {OrderStatus} from "@/types/order";

type Props = {
  status: OrderStatus;
};

export default function OrderStatusBadge({status}: Props) {
  const map: Record<OrderStatus, string> = {
    NEW: "bg-gray-100 text-gray-700",
    PREPARING: "bg-yellow-100 text-yellow-700",
    DONE: "bg-green-100 text-green-700",
    CANCELLED: "bg-red-100 text-red-700"
  };

  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${map[status]}`}>
      {status}
    </span>
  );
}