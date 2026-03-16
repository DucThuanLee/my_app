"use client";

import {useCartStore} from "@/stores/cart-store";

export default function CartIcon() {
  const items = useCartStore((s) => s.items);

  const count = items.reduce((acc, i) => acc + i.quantity, 0);

  return (
    <div className="relative">
      🛒
      {count > 0 && (
        <span className="absolute -top-2 -right-2 rounded-full bg-red-500 px-2 text-xs text-white">
          {count}
        </span>
      )}
    </div>
  );
}