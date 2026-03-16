"use client";

import Link from "next/link";
import {useParams} from "next/navigation";
import {useTranslations} from "next-intl";
import {formatPriceEUR} from "@/lib/api";
import {useCartStore} from "@/stores/cart-store";

/**
 * Cart page (Client Component).
 * - Reads cart state from Zustand
 * - Supports quantity increase/decrease
 * - Supports remove item and clear cart
 * - Shows total price and checkout action
 */
export default function CartPage() {
  const params = useParams<{locale: string}>();
  const locale = params.locale;

  const t = useTranslations("cart");

  const items = useCartStore((state) => state.items);
  const addItem = useCartStore((state) => state.addItem);
  const decreaseItem = useCartStore((state) => state.decreaseItem);
  const removeItem = useCartStore((state) => state.removeItem);
  const clearCart = useCartStore((state) => state.clearCart);

  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);
  const totalPrice = items.reduce(
    (sum, item) => sum + item.product.price * item.quantity,
    0
  );

  return (
    <main className="mx-auto max-w-5xl space-y-8 px-4 pb-24 pt-8">
      {/* ================= PAGE HEADER ================= */}
      <section className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-900">
            {t("title")}
          </h1>
          <p className="mt-1 text-gray-600">{t("subtitle")}</p>
        </div>

        <div className="flex gap-3">
          <Link
            href={`/${locale}/menu`}
            className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50"
          >
            ← {t("continueShopping")}
          </Link>

          {items.length > 0 ? (
            <button
              type="button"
              onClick={clearCart}
              className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 text-sm font-semibold text-red-600 hover:bg-red-50"
            >
              {t("clearCart")}
            </button>
          ) : null}
        </div>
      </section>

      {/* ================= EMPTY STATE ================= */}
      {items.length === 0 ? (
        <section className="rounded-3xl border bg-white p-10 text-center shadow-sm">
          <div className="text-5xl">🛒</div>

          <h2 className="mt-4 text-2xl font-extrabold text-gray-900">
            {t("emptyTitle")}
          </h2>

          <p className="mt-2 text-gray-600">{t("emptySubtitle")}</p>

          <Link
            href={`/${locale}/menu`}
            className="mt-6 inline-flex rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
          >
            {t("browseMenu")}
          </Link>
        </section>
      ) : (
        <>
          {/* ================= CART ITEMS ================= */}
          <section className="space-y-4">
            {items.map((item) => (
              <article
                key={item.product.id}
                className="rounded-3xl border bg-white p-6 shadow-sm"
              >
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  {/* Product details */}
                  <div className="flex-1">
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <h2 className="text-lg font-semibold text-gray-900">
                          {item.product.name}
                        </h2>

                        {item.product.description ? (
                          <p className="mt-2 text-sm text-gray-600">
                            {item.product.description}
                          </p>
                        ) : null}

                        <div className="mt-3 text-xs font-semibold text-gray-500">
                          {t("category")}:{" "}
                          <span className="text-blue-700">
                            {item.product.category}
                          </span>
                        </div>
                      </div>

                      <div className="rounded-full bg-blue-600 px-3 py-1 text-sm font-bold text-white">
                        {formatPriceEUR(item.product.price)}
                      </div>
                    </div>
                  </div>

                  {/* Quantity controls */}
                  <div className="flex flex-col gap-3 sm:items-end">
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        onClick={() => decreaseItem(item.product.id)}
                        className="inline-flex h-10 w-10 items-center justify-center rounded-xl border text-lg font-bold text-blue-700 hover:bg-blue-50"
                        aria-label="Decrease quantity"
                      >
                        −
                      </button>

                      <span className="min-w-10 text-center text-sm font-bold text-gray-900">
                        {item.quantity}
                      </span>

                      <button
                        type="button"
                        onClick={() => addItem(item.product)}
                        className="inline-flex h-10 w-10 items-center justify-center rounded-xl border text-lg font-bold text-blue-700 hover:bg-blue-50"
                        aria-label="Increase quantity"
                      >
                        +
                      </button>
                    </div>

                    <button
                      type="button"
                      onClick={() => removeItem(item.product.id)}
                      className="text-sm font-semibold text-red-600 hover:underline"
                    >
                      {t("remove")}
                    </button>

                    <div className="text-sm font-semibold text-gray-900">
                      {t("subtotal")}:{" "}
                      <span className="text-blue-700">
                        {formatPriceEUR(item.product.price * item.quantity)}
                      </span>
                    </div>
                  </div>
                </div>
              </article>
            ))}
          </section>

          {/* ================= ORDER SUMMARY ================= */}
          <section className="rounded-3xl border bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h2 className="text-xl font-extrabold text-gray-900">
                  {t("summary")}
                </h2>
                <p className="mt-1 text-gray-600">
                  {t("items", {count: totalItems})}
                </p>
              </div>

              <div className="text-right">
                <div className="text-sm text-gray-500">{t("total")}</div>
                <div className="text-2xl font-extrabold text-blue-700">
                  {formatPriceEUR(totalPrice)}
                </div>
              </div>
            </div>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
              <Link
                href={`/${locale}/menu`}
                className="inline-flex items-center justify-center rounded-2xl border px-5 py-3 font-semibold text-blue-700 hover:bg-blue-50"
              >
                {t("continueShopping")}
              </Link>

              <Link
                href={`/${locale}/checkout`}
                className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
              >
                {t("goToCheckout")} →
              </Link>
            </div>
          </section>
        </>
      )}
    </main>
  );
}