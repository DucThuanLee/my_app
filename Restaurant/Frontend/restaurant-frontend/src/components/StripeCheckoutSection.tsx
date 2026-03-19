"use client";

import {Elements} from "@stripe/react-stripe-js";
import {loadStripe} from "@stripe/stripe-js";
import StripeCheckoutForm from "@/components/StripeCheckoutForm";

const stripePromise = loadStripe(
  process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY || ""
);

type Props = {
  clientSecret: string;
  returnUrl: string;
  submitLabel: string;
  processingLabel: string;
};

export default function StripeCheckoutSection({
  clientSecret,
  returnUrl,
  submitLabel,
  processingLabel
}: Props) {
  return (
    <Elements
      stripe={stripePromise}
      options={{clientSecret}}
    >
      <StripeCheckoutForm
        returnUrl={returnUrl}
        submitLabel={submitLabel}
        processingLabel={processingLabel}
      />
    </Elements>
  );
}