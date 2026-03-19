import Link from "next/link";
import {getTranslations} from "next-intl/server";

import {formatPriceEUR} from "@/lib/http";
import {getOrderById} from "@/lib/order-api";
import {getStripePaymentStatus} from "@/lib/payment-api";

import OrderStatusLive from "@/components/OrderStatusLive";

type Props = {
  params: Promise<{locale: string; id: string}>;
};

export default async function OrderDetailPage({params}: Props) {
  const {locale, id} = await params;
  const t = await getTranslations("order");

  const order = await getOrderById(id);

  let initialStatus = {
    orderId: order.id,
    stripePaymentIntentId: "",
    paymentStatus: order.paymentStatus,
    orderStatus: order.orderStatus
  };

  try {
    initialStatus = await getStripePaymentStatus(id);
  } catch (error) {
    console.error("Could not fetch Stripe payment status:", error);
  }

  return (
    <main className="mx-auto max-w-4xl space-y-8 px-4 pb-24 pt-8">
      <section className="rounded-3xl border bg-white p-8 shadow-sm">
        <div className="text-5xl">✅</div>

        <h1 className="mt-4 text-3xl font-extrabold text-gray-900">
          {t("title")}
        </h1>

        <p className="mt-2 text-gray-600">
          {t("subtitle")} #{order.id}
        </p>

        <OrderStatusLive
          orderId={order.id}
          initialStatus={initialStatus}
        />
      </section>

      <section className="rounded-3xl border bg-white p-6 shadow-sm">
        <h2 className="text-xl font-extrabold text-gray-900">
          {t("summary")}
        </h2>

        <div className="mt-5 space-y-4">
          {order.items.map((item) => (
            <div
              key={item.productId}
              className="flex items-start justify-between gap-3"
            >
              <div>
                <div className="font-semibold text-gray-900">
                  {item.productName} × {item.quantity}
                </div>

                <div className="mt-1 text-sm text-gray-500">
                  {formatPriceEUR(item.price)} / item
                </div>
              </div>

              <div className="font-semibold text-blue-700">
                {formatPriceEUR(item.price * item.quantity)}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 border-t pt-5">
          <div className="flex items-center justify-between text-lg font-extrabold text-gray-900">
            <span>{t("total")}</span>
            <span className="text-blue-700">
              {formatPriceEUR(order.totalPrice)}
            </span>
          </div>
        </div>
      </section>

      <div className="flex flex-col gap-3 sm:flex-row">
        <Link
          href={`/${locale}/menu`}
          className="inline-flex items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
        >
          {t("backToMenu")}
        </Link>

        <Link
          href={`/${locale}`}
          className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700"
        >
          {t("backHome")}
        </Link>
      </div>
    </main>
  );
}