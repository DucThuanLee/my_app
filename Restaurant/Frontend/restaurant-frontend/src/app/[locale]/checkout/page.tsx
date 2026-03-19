"use client";

import Link from "next/link";
import {useMemo, useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {useTranslations} from "next-intl";
import {createOrder, formatPriceEUR} from "@/lib/api";
import {useCartStore} from "@/stores/cart-store";
import {PaymentMethod} from "@/types/order";
import type {CreateOrderRequest} from "@/types/order";

export default function CheckoutPage() {
  const params = useParams<{locale: string}>();
  const locale = params.locale;
  const router = useRouter();

  const t = useTranslations("checkout");

  const items = useCartStore((state) => state.items);
  const clearCart = useCartStore((state) => state.clearCart);

  const [customerName, setCustomerName] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>(PaymentMethod.COD);

  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const totalItems = useMemo(
    () => items.reduce((sum, item) => sum + item.quantity, 0),
    [items]
  );

  const subtotal = useMemo(
    () => items.reduce((sum, item) => sum + item.product.price * item.quantity, 0),
    [items]
  );

  const deliveryFee = subtotal > 0 ? 2.5 : 0;
  const total = subtotal + deliveryFee;

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setErrorMessage("");
    setSuccessMessage("");

    if (items.length === 0) {
      setErrorMessage(t("errorEmptyCart"));
      return;
    }

    if (!customerName.trim() || !phone.trim() || !address.trim()) {
      setErrorMessage(t("errorRequiredFields"));
      return;
    }

    const payload: CreateOrderRequest = {
      customerName: customerName.trim(),
      phone: phone.trim(),
      address: address.trim(),
      paymentMethod,
      items: items.map((item) => ({
        productId: item.product.id,
        quantity: item.quantity
      }))
    };

    try {
      setSubmitting(true);

      await createOrder(payload);

      clearCart();
      setSuccessMessage(t("successOrderCreated"));

      window.setTimeout(() => {
        router.push(`/${locale}`);
      }, 1200);
    } catch (error) {
      console.error(error);
      setErrorMessage(t("errorCreateOrder"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
      <section className="space-y-2">
        <h1 className="text-3xl font-extrabold text-gray-900">{t("title")}</h1>
        <p className="text-gray-600">{t("subtitle")}</p>
      </section>

      {items.length === 0 ? (
        <section className="rounded-3xl border bg-white p-10 text-center shadow-sm">
          <div className="text-5xl">🧾</div>
          <h2 className="mt-4 text-2xl font-extrabold text-gray-900">{t("emptyTitle")}</h2>
          <p className="mt-2 text-gray-600">{t("emptySubtitle")}</p>

          <Link
            href={`/${locale}/menu`}
            className="mt-6 inline-flex rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
          >
            {t("browseMenu")}
          </Link>
        </section>
      ) : (
        <form onSubmit={handleSubmit} className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
          <section className="rounded-3xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-extrabold text-gray-900">{t("customerInfo")}</h2>

            <div className="mt-5 grid gap-4">
              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">
                  {t("fullName")}
                </label>
                <input
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  className="w-full rounded-2xl border px-4 py-3"
                  placeholder={t("fullName")}
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">
                  {t("phone")}
                </label>
                <input
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full rounded-2xl border px-4 py-3"
                  placeholder={t("phone")}
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-gray-700">
                  {t("address")}
                </label>
                <input
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="w-full rounded-2xl border px-4 py-3"
                  placeholder={t("address")}
                />
              </div>
            </div>

            <h2 className="mt-8 text-xl font-extrabold text-gray-900">{t("paymentMethod")}</h2>

            <div className="mt-4 grid gap-3">
              <label className="flex items-center gap-3 rounded-2xl border p-4">
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === PaymentMethod.COD}
                  onChange={() => setPaymentMethod(PaymentMethod.COD)}
                />
                <span className="font-semibold text-gray-900">{t("paymentCash")}</span>
              </label>

              <label className="flex items-center gap-3 rounded-2xl border p-4">
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === PaymentMethod.PAYPAL}
                  onChange={() => setPaymentMethod(PaymentMethod.PAYPAL)}
                />
                <span className="font-semibold text-gray-900">{t("paymentPaypal")}</span>
              </label>

              <label className="flex items-center gap-3 rounded-2xl border p-4">
                <input
                  type="radio"
                  name="payment"
                  checked={paymentMethod === PaymentMethod.STRIPE}
                  onChange={() => setPaymentMethod(PaymentMethod.STRIPE)}
                />
                <span className="font-semibold text-gray-900">{t("paymentStripe")}</span>
              </label>
            </div>

            {errorMessage ? (
              <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {errorMessage}
              </div>
            ) : null}

            {successMessage ? (
              <div className="mt-6 rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
                {successMessage}
              </div>
            ) : null}
          </section>

          <aside className="rounded-3xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-extrabold text-gray-900">{t("summary")}</h2>

            <div className="mt-5 space-y-4">
              {items.map((item) => (
                <div key={item.product.id} className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="font-semibold text-gray-900">
                      {item.product.name} × {item.quantity}
                    </div>
                    {item.product.description ? (
                      <div className="mt-1 line-clamp-1 text-sm text-gray-500">
                        {item.product.description}
                      </div>
                    ) : null}
                  </div>

                  <div className="shrink-0 font-semibold text-blue-700">
                    {formatPriceEUR(item.product.price * item.quantity)}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 space-y-3 border-t pt-5 text-sm">
              <div className="flex items-center justify-between text-gray-600">
                <span>{t("items", {count: totalItems})}</span>
                <span>{formatPriceEUR(subtotal)}</span>
              </div>

              <div className="flex items-center justify-between text-gray-600">
                <span>{t("deliveryFee")}</span>
                <span>{formatPriceEUR(deliveryFee)}</span>
              </div>

              <div className="flex items-center justify-between text-lg font-extrabold text-gray-900">
                <span>{t("total")}</span>
                <span className="text-blue-700">{formatPriceEUR(total)}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 inline-flex w-full items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? t("placingOrder") : t("placeOrder")}
            </button>

            <Link
              href={`/${locale}/cart`}
              className="mt-3 inline-flex w-full items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
            >
              {t("backToCart")}
            </Link>
          </aside>
        </form>
      )}
    </main>
  );
}