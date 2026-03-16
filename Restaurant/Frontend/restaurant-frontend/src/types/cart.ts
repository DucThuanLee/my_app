import type {Product} from "./product";

/**
 * Cart item stored in Zustand
 */
export type CartItem = {
  product: Product;
  quantity: number;
};