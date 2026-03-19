export type CreateStripeIntentRequest = {
    orderId: string;
  };
  
  export type CreateStripeIntentResponse = {
    paymentIntentId: string;
    clientSecret: string;
  };
  
  export type PaymentStatusResponse = {
    orderId: string;
    stripePaymentIntentId: string;
    paymentStatus: string;
    orderStatus: string;
  };