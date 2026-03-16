"use client";

import {useCartStore} from "../stores/cart-store"; // Adjusted path to relative
import type {Product} from "@/types/product";

type Props = {
  product: Product;
  label: string;
};

/**
 * Client component for adding product to cart.
 * Used in both Home and Menu pages.
 */
export default function AddToCartButton({product, label}: Props) {
  const addItem = useCartStore((s) => s.addItem);

  return (
    <button
      onClick={() => addItem(product)}
      className="flex-1 rounded-2xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
    >
      {label}
    </button>
  );
}