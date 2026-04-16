"use client";

import {useState} from "react";
import RefundModal from "./RefundModal";

export default function RefundButton({
  orderId,
  total,
  disabled,
  onSuccess
}: {
  orderId: string;
  total: number;
  disabled?: boolean;
  onSuccess?: () => void;
}) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        disabled={disabled}
        className="rounded-xl bg-purple-600 px-4 py-2 text-sm font-semibold text-white hover:bg-purple-700 disabled:opacity-50"
      >
        Refund
      </button>

      <RefundModal
        open={open}
        orderId={orderId}
        maxAmount={total}
        onClose={() => setOpen(false)}
        onSuccess={onSuccess}
      />
    </>
  );
}