type Props = {
  status: string;
};

export default function PaymentBadge({status}: Props) {
  const map: Record<string, string> = {
    PENDING: "bg-gray-100 text-gray-700",
    PAID: "bg-green-100 text-green-700",
    FAILED: "bg-red-100 text-red-700",
    CANCELED: "bg-orange-100 text-orange-700",
    REFUNDED: "bg-purple-100 text-purple-700"
  };

  return (
    <span className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${map[status] ?? "bg-gray-100 text-gray-700"}`}>
      {status}
    </span>
  );
}