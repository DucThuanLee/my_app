"use client";

import {useState} from "react";
import {PaymentElement, useElements, useStripe} from "@stripe/react-stripe-js";

type Props = {
  returnUrl: string;
  submitLabel: string;
  processingLabel: string;
};

export default function StripeCheckoutForm({
  returnUrl,
  submitLabel,
  processingLabel
}: Props) {
  const stripe = useStripe();
  const elements = useElements();

  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  async function handleStripePayment() {
    if (!stripe || !elements) return;

    setSubmitting(true);
    setErrorMessage("");

    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: returnUrl
      }
    });

    if (result.error) {
      setErrorMessage(result.error.message ?? "Payment failed");
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-4">
      <PaymentElement />

      {errorMessage ? (
        <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      <button
        type="button"
        onClick={handleStripePayment}
        disabled={!stripe || !elements || submitting}
        className="inline-flex w-full items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
      >
        {submitting ? processingLabel : submitLabel}
      </button>
    </div>
  );
}