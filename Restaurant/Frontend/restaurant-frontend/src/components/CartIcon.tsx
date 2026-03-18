"use client";

import {useCartStore} from "@/stores/cart-store";

export default function CartIcon() {
  const items = useCartStore((state) => state.items);

  const count = items.reduce((total, item) => total + item.quantity, 0);

  return (
    <div className="relative flex items-center justify-center">
      <span className="text-xl" aria-hidden>
        🛒
      </span>

      {count > 0 ? (
        <span className="absolute -right-2 -top-2 min-w-5 rounded-full bg-red-500 px-1.5 py-0.5 text-center text-[10px] font-bold leading-none text-white">
          {count}
        </span>
      ) : null}
    </div>
  );
}