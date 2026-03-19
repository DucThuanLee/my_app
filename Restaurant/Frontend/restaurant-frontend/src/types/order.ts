import type {Product} from "./product";

export enum OrderStatus {
  NEW = "NEW",
  PREPARING = "PREPARING",
  DONE = "DONE",
  CANCELLED = "CANCELLED"
}

export enum PaymentStatus {
  PENDING = "PENDING",
  PAID = "PAID",
  FAILED = "FAILED",
  CANCELED = "CANCELED",
  REFUNDED = "REFUNDED"
}

export enum PaymentMethod {
  STRIPE = "STRIPE",
  PAYPAL = "PAYPAL",
  COD = "COD"
}

export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  price: number;
  product?: Product;
}

export interface Order {
  id: string;
  totalPrice: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  createdAt: string;
  items: OrderItem[];
}

export interface CreateOrderRequest {
  customerName: string;
  phone: string;
  address: string;
  paymentMethod: PaymentMethod;
  items: {
    productId: string;
    quantity: number;
  }[];
}