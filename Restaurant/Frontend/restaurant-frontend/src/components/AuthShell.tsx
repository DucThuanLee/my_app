import type {ReactNode} from "react";
import Link from "next/link";

type Props = {
  locale: string;
  badge: string;
  title: string;
  subtitle: string;
  children: ReactNode;
  footerText: string;
  footerLinkLabel: string;
  footerHref: string;
};

export default function AuthShell({
  locale,
  badge,
  title,
  subtitle,
  children,
  footerText,
  footerLinkLabel,
  footerHref
}: Props) {
  return (
    <main className="mx-auto max-w-6xl px-4 py-10">
      <div className="grid gap-8 lg:grid-cols-[1fr_440px] lg:items-center">
        <section className="relative overflow-hidden rounded-3xl border bg-white p-8 shadow-sm md:p-10">
          <div className="pointer-events-none absolute -top-20 -right-20 h-64 w-64 rounded-full bg-blue-200/60 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-sky-200/50 blur-3xl" />

          <div className="relative space-y-5">
            <span className="inline-flex rounded-full bg-blue-50 px-4 py-1 text-sm font-semibold text-blue-700">
              {badge}
            </span>

            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 md:text-5xl">
              {title}
            </h1>

            <p className="max-w-xl text-lg leading-8 text-gray-600">
              {subtitle}
            </p>

            <div className="grid gap-4 pt-4 sm:grid-cols-3">
              <div className="rounded-2xl border bg-white p-4">
                <div className="text-sm font-semibold text-blue-700">Secure</div>
                <div className="mt-1 text-sm text-gray-600">
                  Protected accounts and safe sign-in flow.
                </div>
              </div>

              <div className="rounded-2xl border bg-white p-4">
                <div className="text-sm font-semibold text-blue-700">Fast</div>
                <div className="mt-1 text-sm text-gray-600">
                  Quick access to your orders and checkout.
                </div>
              </div>

              <div className="rounded-2xl border bg-white p-4">
                <div className="text-sm font-semibold text-blue-700">Convenient</div>
                <div className="mt-1 text-sm text-gray-600">
                  Save details and manage your orders more easily.
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="rounded-3xl border bg-white p-6 shadow-sm md:p-8">
          {children}

          <div className="mt-6 border-t pt-5 text-sm text-gray-600">
            {footerText}{" "}
            <Link
              href={`/${locale}${footerHref}`}
              className="font-semibold text-blue-700 hover:underline"
            >
              {footerLinkLabel}
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}