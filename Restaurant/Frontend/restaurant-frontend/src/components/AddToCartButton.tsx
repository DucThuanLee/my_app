"use client";

import {useEffect, useRef, useState} from "react";
import {useCartStore} from "@/stores/cart-store";
import type {Product} from "@/types/product";

type Props = {
  product: Product;
  label: string;
  addedLabel?: string;
  disabled?: boolean;
};

/**
 * Production-ready add-to-cart button.
 * - Adds product to Zustand cart store
 * - Shows temporary "Added" state
 * - Prevents rapid repeated clicks for a short moment
 */
export default function AddToCartButton({
  product,
  label,
  addedLabel = "Added",
  disabled = false
}: Props) {
  const addItem = useCartStore((state) => state.addItem);

  const [justAdded, setJustAdded] = useState(false);
  const timeoutRef = useRef<number | null>(null);

  function handleClick() {
    if (disabled || justAdded) return;

    addItem(product);
    setJustAdded(true);

    timeoutRef.current = window.setTimeout(() => {
      setJustAdded(false);
    }, 1200);
  }

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, []);

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled}
      className={[
        "inline-flex flex-1 items-center justify-center rounded-2xl px-4 py-2 text-sm font-semibold text-white transition",
        disabled
          ? "cursor-not-allowed bg-gray-300"
          : justAdded
            ? "bg-green-600 hover:bg-green-700"
            : "bg-blue-600 hover:bg-blue-700"
      ].join(" ")}
      aria-label={justAdded ? addedLabel : label}
    >
      {justAdded ? addedLabel : label}
    </button>
  );
}