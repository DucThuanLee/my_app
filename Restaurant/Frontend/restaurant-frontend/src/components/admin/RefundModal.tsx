"use client";

import {useState} from "react";
import {motion, AnimatePresence} from "framer-motion";
import {refundOrder} from "@/lib/admin-api";
import {useToast} from "@/components/ui/ToastProvider";

export default function RefundModal({
  open,
  orderId,
  maxAmount,
  onClose,
  onSuccess
}: {
  open: boolean;
  orderId: string;
  maxAmount: number;
  onClose: () => void;
  onSuccess?: () => void;
}) {
  const {show} = useToast();

  const [amount, setAmount] = useState(maxAmount);
  const [reason, setReason] = useState("customer_request");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleRefund() {
    if (amount <= 0) {
      setError("Amount must be > 0");
      return;
    }

    try {
      setLoading(true);
      setError("");

      await refundOrder({
        orderId,
        amount,
        reason
      });

      show("Refund successful 💸");
      onSuccess?.();
      onClose();
    } catch {
      setError("Refund failed");
      show("Refund failed ❌");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
          initial={{opacity: 0}}
          animate={{opacity: 1}}
          exit={{opacity: 0}}
        >
          <motion.div
            initial={{scale: 0.9, opacity: 0}}
            animate={{scale: 1, opacity: 1}}
            exit={{scale: 0.9, opacity: 0}}
            transition={{duration: 0.2}}
            className="w-full max-w-md rounded-3xl bg-white p-6 shadow-xl"
          >
            <h2 className="text-xl font-extrabold text-gray-900">
              Refund payment
            </h2>

            <p className="text-sm text-gray-500">
              Issue full or partial refund
            </p>

            {/* Amount */}
            <div className="mt-6">
              <label className="text-sm font-semibold text-gray-700">
                Amount (€)
              </label>

              <div className="mt-2 flex items-center rounded-xl border px-4 py-3">
                <span className="mr-2 text-gray-500">€</span>
                <input
                  type="number"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                  className="w-full outline-none"
                />
              </div>

              <p className="mt-1 text-xs text-gray-500">
                Max: €{maxAmount.toFixed(2)}
              </p>
            </div>

            {/* Reason */}
            <div className="mt-4">
              <label className="text-sm font-semibold text-gray-700">
                Reason
              </label>

              <select
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="mt-2 w-full rounded-xl border px-4 py-3"
              >
                <option value="customer_request">Customer request</option>
                <option value="duplicate">Duplicate</option>
                <option value="fraud">Fraud</option>
              </select>
            </div>

            {/* Error */}
            {error && (
              <div className="mt-4 rounded-xl bg-red-50 px-4 py-2 text-sm text-red-700">
                {error}
              </div>
            )}

            {/* Actions */}
            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={onClose}
                className="rounded-xl px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-100"
              >
                Cancel
              </button>

              <button
                onClick={handleRefund}
                disabled={loading}
                className="rounded-xl bg-purple-600 px-5 py-2 text-sm font-semibold text-white hover:bg-purple-700 disabled:opacity-50"
              >
                {loading ? "Processing..." : "Refund"}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}