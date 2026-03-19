"use client";

import Link from "next/link";
import {useEffect, useMemo, useState} from "react";
import {createPortal} from "react-dom";
import {formatPriceEUR} from "@/lib/api";
import {useCartStore} from "@/stores/cart-store";

type Props = {
  locale: string;
  title: string;
  emptyTitle: string;
  emptySubtitle: string;
  continueShoppingLabel: string;
  clearCartLabel: string;
  subtotalLabel: string;
  totalLabel: string;
  checkoutLabel: string;
  removeLabel: string;
  decreaseLabel: string;
  increaseLabel: string;
};

export default function CartDrawer({
  locale,
  title,
  emptyTitle,
  emptySubtitle,
  continueShoppingLabel,
  clearCartLabel,
  subtotalLabel,
  totalLabel,
  checkoutLabel,
  removeLabel,
  decreaseLabel,
  increaseLabel
}: Props) {
  const [open, setOpen] = useState(false);
  const [mounted, setMounted] = useState(false);

  const items = useCartStore((state) => state.items);
  const addItem = useCartStore((state) => state.addItem);
  const decreaseItem = useCartStore((state) => state.decreaseItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const clearCart = useCartStore((state) => state.clearCart);

  const totalItems = useMemo(
    () => items.reduce((sum, item) => sum + item.quantity, 0),
    [items]
  );

  const totalPrice = useMemo(
    () => items.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
    [items]
  );

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!open) return;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [open]);

  const drawer = open ? (
    <div className="fixed inset-0 z-[9999]">
      <button
        type="button"
        className="absolute inset-0 bg-black/40"
        onClick={() => setOpen(false)}
        aria-label="Close cart drawer"
      />

      <aside className="absolute right-0 top-0 flex h-full w-full max-w-md flex-col border-l bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b px-5 py-4">
          <div>
            <h2 className="text-lg font-extrabold text-gray-900">{title}</h2>
            <p className="text-sm text-gray-500">
              {totalItems} item{totalItems === 1 ? "" : "s"}
            </p>
          </div>

          <button
            type="button"
            onClick={() => setOpen(false)}
            className="rounded-xl border px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            ✕
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          {items.length === 0 ? (
            <div className="rounded-3xl border bg-white p-8 text-center">
              <div className="text-5xl">🛒</div>
              <h3 className="mt-4 text-xl font-extrabold text-gray-900">
                {emptyTitle}
              </h3>
              <p className="mt-2 text-sm text-gray-600">{emptySubtitle}</p>

              <Link
                href={`/${locale}/menu`}
                onClick={() => setOpen(false)}
                className="mt-5 inline-flex rounded-2xl bg-blue-600 px-5 py-3 font-semibold text-white hover:bg-blue-700"
              >
                {continueShoppingLabel}
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {items.map((item) => (
                <div
                  key={item.product.id}
                  className="rounded-3xl border bg-white p-4 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <h3 className="font-semibold text-gray-900">
                        {item.product.name}
                      </h3>
                      {item.product.description ? (
                        <p className="mt-1 line-clamp-2 text-sm text-gray-600">
                          {item.product.description}
                        </p>
                      ) : null}
                    </div>

                    <div className="shrink-0 rounded-full bg-blue-600 px-3 py-1 text-xs font-bold text-white">
                      {formatPriceEUR(item.product.price)}
                    </div>
                  </div>

                  <div className="mt-4 flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => decreaseItem(item.product.id)}
                        className="inline-flex h-9 w-9 items-center justify-center rounded-xl border text-lg font-bold text-blue-700 hover:bg-blue-50"
                        aria-label={decreaseLabel}
                      >
                        −
                      </button>

                      <span className="min-w-8 text-center text-sm font-bold text-gray-900">
                        {item.quantity}
                      </span>

                      <button
                        type="button"
                        onClick={() => addItem(item.product)}
                        className="inline-flex h-9 w-9 items-center justify-center rounded-xl border text-lg font-bold text-blue-700 hover:bg-blue-50"
                        aria-label={increaseLabel}
                      >
                        +
                      </button>
                    </div>

                    <button
                      type="button"
                      onClick={() => removeItem(item.product.id)}
                      className="text-sm font-semibold text-red-600 hover:underline"
                    >
                      {removeLabel}
                    </button>
                  </div>

                  <div className="mt-3 text-sm font-semibold text-gray-700">
                    {subtotalLabel}:{" "}
                    <span className="text-blue-700">
                      {formatPriceEUR(item.product.price * item.quantity)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="border-t bg-white p-4">
          <div className="mb-4 flex items-center justify-between">
            <span className="text-sm text-gray-500">{totalLabel}</span>
            <span className="text-xl font-extrabold text-blue-700">
              {formatPriceEUR(totalPrice)}
            </span>
          </div>

          <div className="flex gap-3">
            <button
              type="button"
              onClick={clearCart}
              className="inline-flex flex-1 items-center justify-center rounded-2xl border px-4 py-3 font-semibold text-red-600 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={items.length === 0}
            >
              {clearCartLabel}
            </button>

            <Link
              href={`/${locale}/checkout`}
              onClick={() => setOpen(false)}
              className="inline-flex flex-1 items-center justify-center rounded-2xl bg-blue-600 px-4 py-3 font-semibold text-white hover:bg-blue-700"
            >
              {checkoutLabel}
            </Link>
          </div>
        </div>
      </aside>
    </div>
  ) : null;

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex items-center justify-center rounded-xl border bg-white px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
        aria-label={title}
      >
        <div className="relative flex items-center justify-center">
          <span className="text-xl" aria-hidden>
            🛒
          </span>

          {totalItems > 0 ? (
            <span className="absolute -right-2 -top-2 min-w-5 rounded-full bg-red-500 px-1.5 py-0.5 text-center text-[10px] font-bold leading-none text-white">
              {totalItems}
            </span>
          ) : null}
        </div>
      </button>

      {mounted ? createPortal(drawer, document.body) : null}
    </>
  );
}