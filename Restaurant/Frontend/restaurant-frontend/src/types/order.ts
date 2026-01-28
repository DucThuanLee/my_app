import { Product } from './product';

export enum OrderStatus {
  NEW = 'NEW',
  PREPARING = 'PREPARING',
  DONE = 'DONE',
  CANCELLED = 'CANCELLED'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED'
}

export enum PaymentMethod {
  STRIPE = 'STRIPE',
  PAYPAL = 'PAYPAL',
  COD = 'COD'
}

export interface OrderItem {
  id: string;
  productId: string;
  quantity: number;
  price: number;
  product?: Product;
}

export interface Order {
  id: string;
  userId?: string;
  customerName: string;
  phone: string;
  address: string;
  totalPrice: number;
  paymentMethod: PaymentMethod;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  items: OrderItem[];
  createdAt: string;
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