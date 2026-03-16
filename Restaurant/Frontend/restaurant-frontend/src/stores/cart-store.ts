"use client";

import {create} from "zustand";
import type {CartItem} from "@/types/cart";
import type {Product} from "@/types/product";

type CartState = {
  items: CartItem[];
  addItem: (product: Product) => void;
  decreaseItem: (productId: string) => void;
  removeItem: (productId: string) => void;
  clearCart: () => void;
};

export const useCartStore = create<CartState>((set) => ({
  items: [],

  addItem: (product) =>
    set((state) => {
      const existing = state.items.find((i) => i.product.id === product.id);

      if (existing) {
        return {
          items: state.items.map((item) =>
            item.product.id === product.id
              ? {...item, quantity: item.quantity + 1}
              : item
          )
        };
      }

      return {
        items: [...state.items, {product, quantity: 1}]
      };
    }),

  decreaseItem: (productId) =>
    set((state) => {
      const existing = state.items.find((i) => i.product.id === productId);

      if (!existing) return state;

      if (existing.quantity <= 1) {
        return {
          items: state.items.filter((i) => i.product.id !== productId)
        };
      }

      return {
        items: state.items.map((item) =>
          item.product.id === productId
            ? {...item, quantity: item.quantity - 1}
            : item
        )
      };
    }),

  removeItem: (productId) =>
    set((state) => ({
      items: state.items.filter((i) => i.product.id !== productId)
    })),

  clearCart: () => set({items: []})
}));