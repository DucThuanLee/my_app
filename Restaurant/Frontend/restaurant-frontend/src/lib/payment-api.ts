import type {
    CreateStripeIntentRequest,
    CreateStripeIntentResponse,
    PaymentStatusResponse
  } from "@/types/payment";
  import {backendUrl, readJsonOrThrow} from "@/lib/http";
  
  export async function createStripeIntent(
    payload: CreateStripeIntentRequest
  ): Promise<CreateStripeIntentResponse> {
    const url = backendUrl("/api/payments/stripe/intents");
  
    const res = await fetch(url.toString(), {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(payload)
    });
  
    return readJsonOrThrow<CreateStripeIntentResponse>(
      res,
      "Failed to create Stripe payment intent"
    );
  }
  
  export async function getStripePaymentStatus(
    orderId: string
  ): Promise<PaymentStatusResponse> {
    const url = backendUrl(`/api/payments/stripe/status/${orderId}`);
  
    const res = await fetch(url.toString(), {
      cache: "no-store"
    });
  
    return readJsonOrThrow<PaymentStatusResponse>(
      res,
      "Failed to fetch Stripe payment status"
    );
  }