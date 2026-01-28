import Link from "next/link";
import { getTranslations } from "next-intl/server";

/**
 * Modern Food Ordering homepage (Server Component).
 * - Pattern: Big hero + categories + best sellers (horizontal) + info + footer
 * - Adds app-like elements: horizontal scroll + sticky bottom CTA on mobile
 * - Uses next-intl nested namespace: "home"
 */
export default async function HomePage({
    params
}: {
    params: Promise<{ locale: string }>;
}) {
    const { locale } = await params;
    const t = await getTranslations("home");

    return (
        <div className="min-h-screen bg-gradient-to-b from-blue-50/60 via-white to-white">
            {/* ================= STICKY NAVBAR ================= */}
            <header className="sticky top-0 z-40 border-b bg-white/80 backdrop-blur">
                <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
                    {/* Brand */}
                    <Link href={`/${locale}`} className="flex items-center gap-2">
                        <span className="inline-flex h-9 w-9 items-center justify-center rounded-2xl bg-blue-600 font-extrabold text-white">
                            M
                        </span>
                        <div className="leading-tight">
                            <div className="text-sm font-extrabold text-gray-900">
                                {t("brand")}
                            </div>
                            <div className="text-xs text-gray-500">
                                {t("badge")}
                            </div>
                        </div>
                    </Link>

                    {/* Actions */}
                    <nav className="flex items-center gap-2">
                        <Link
                            href={`/${locale}/menu`}
                            className="hidden rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50 md:inline-flex"
                        >
                            {t("ctaMenu")}
                        </Link>

                        <Link
                            href={`/${locale}/checkout`}
                            className="hidden rounded-xl bg-blue-600 px-3 py-2 text-sm font-semibold text-white hover:bg-blue-700 md:inline-flex"
                        >
                            {t("ctaOrder")}
                        </Link>

                        {/* Simple locale switch links (no client code) */}
                        <div className="hidden items-center gap-1 rounded-xl border px-2 py-1 text-xs font-semibold text-gray-600 md:flex">
                            <Link
                                href="/de"
                                className={`rounded-lg px-2 py-1 ${locale === "de" ? "bg-blue-50 text-blue-700" : "hover:bg-gray-50"}`}
                            >
                                DE
                            </Link>
                            <Link
                                href="/en"
                                className={`rounded-lg px-2 py-1 ${locale === "en" ? "bg-blue-50 text-blue-700" : "hover:bg-gray-50"}`}
                            >
                                EN
                            </Link>
                        </div>

                        {/* Cart shortcut (placeholder) */}
                        <Link
                            href={`/${locale}/checkout`}
                            className="inline-flex items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
                            aria-label="Go to checkout"
                        >
                            <span>🛒</span>
                            <span className="hidden sm:inline">{t("ctaOrder")}</span>
                        </Link>
                    </nav>
                </div>
            </header>

            <main className="mx-auto max-w-6xl space-y-14 px-4 pb-24 pt-8 md:pb-16">
                {/* ================= HERO ================= */}
                <section className="relative overflow-hidden rounded-3xl border bg-white p-8 md:p-12">
                    {/* Decorative gradients */}
                    <div className="pointer-events-none absolute -top-24 -right-24 h-72 w-72 rounded-full bg-blue-200/60 blur-3xl" />
                    <div className="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-sky-200/60 blur-3xl" />

                    <div className="relative grid gap-10 md:grid-cols-2 md:items-center">
                        {/* Copy */}
                        <div className="space-y-5">
                            <span className="inline-flex items-center gap-2 rounded-full bg-blue-50 px-4 py-1 text-sm font-semibold text-blue-700">
                                {t("badge")}
                                <span className="inline-block h-1.5 w-1.5 rounded-full bg-blue-600" />
                                {t("badge2")}
                            </span>

                            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 md:text-5xl">
                                {t("heroTitle")}
                                <br />
                                <span className="text-blue-700">{t("heroTitle2")}</span>
                            </h1>

                            <p className="max-w-prose text-lg leading-relaxed text-gray-600">
                                {t("heroSubtitle")}
                            </p>

                            <div className="flex flex-col gap-3 sm:flex-row">
                                <Link
                                    href={`/${locale}/menu`}
                                    className="inline-flex items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white shadow-sm hover:bg-blue-700"
                                >
                                    {t("ctaMenu")}
                                </Link>
                                <Link
                                    href={`/${locale}/checkout`}
                                    className="inline-flex items-center justify-center rounded-2xl border px-6 py-3 font-semibold text-blue-700 hover:bg-blue-50"
                                >
                                    {t("ctaOrder")}
                                </Link>
                            </div>

                            {/* Trust chips */}
                            <div className="flex flex-wrap gap-2 text-sm text-gray-600">
                                <span className="rounded-full border bg-white px-3 py-1">✓ {t("perkFast")}</span>
                                <span className="rounded-full border bg-white px-3 py-1">✓ {t("perkGuest")}</span>
                                <span className="rounded-full border bg-white px-3 py-1">✓ {t("perkPayment")}</span>
                            </div>
                        </div>

                        {/* Popular card */}
                        <div className="relative">
                            <div className="rounded-3xl border bg-gradient-to-b from-blue-50 to-white p-6 shadow-sm">
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between">
                                        <span className="text-sm font-semibold text-blue-700">
                                            {t("popularToday")}
                                        </span>
                                        <span className="rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold text-white">
                                            {t("bestSellerTag")}
                                        </span>
                                    </div>

                                    {/* Top items preview */}
                                    <div className="space-y-3">
                                        {[
                                            { name: t("item1"), price: "5.20 €" },
                                            { name: t("item2"), price: "4.50 €" },
                                            { name: t("item3"), price: "6.90 €" }
                                        ].map((item) => (
                                            <div
                                                key={item.name}
                                                className="flex items-center justify-between rounded-2xl border bg-white p-4"
                                            >
                                                <span className="font-semibold text-gray-900">{item.name}</span>
                                                <span className="font-bold text-blue-700">{item.price}</span>
                                            </div>
                                        ))}
                                    </div>

                                    {/* Delivery estimate */}
                                    <div className="rounded-2xl bg-blue-600 p-4 text-white">
                                        <div className="text-sm opacity-90">{t("etaLabel")}</div>
                                        <div className="text-lg font-bold">{t("etaValue")}</div>
                                    </div>
                                </div>
                            </div>

                            <div className="pointer-events-none absolute -bottom-8 -right-8 h-28 w-28 rounded-full bg-blue-300/50 blur-2xl" />
                        </div>
                    </div>
                </section>

                {/* ================= CATEGORIES ================= */}
                <section className="space-y-5">
                    <div className="flex items-end justify-between">
                        <div>
                            <h2 className="text-2xl font-extrabold text-gray-900">
                                {t("categories")}
                            </h2>
                            <p className="mt-1 text-gray-600">
                                {t("categoriesSubtitle")}
                            </p>
                        </div>

                        <Link
                            href={`/${locale}/menu`}
                            className="hidden rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:inline-flex"
                        >
                            {t("seeAll")} →
                        </Link>
                    </div>

                    <div className="grid gap-4 md:grid-cols-3">
                        {[
                            {
                                key: "bubbletea",
                                title: t("catBubbleTea"),
                                desc: t("catBubbleTeaDesc"),
                                emoji: "🧋"
                            },
                            {
                                key: "coffee",
                                title: t("catCoffee"),
                                desc: t("catCoffeeDesc"),
                                emoji: "☕"
                            },
                            {
                                key: "chicken",
                                title: t("catChicken"),
                                desc: t("catChickenDesc"),
                                emoji: "🍗"
                            }
                        ].map((c) => (
                            <div key={c.key} className="rounded-3xl border bg-white p-6 shadow-sm">
                                <div className="flex items-start justify-between">
                                    <h3 className="text-lg font-semibold text-blue-700">
                                        {c.title}
                                    </h3>
                                    <span className="text-2xl" aria-hidden>
                                        {c.emoji}
                                    </span>
                                </div>

                                <p className="mt-2 text-gray-600">{c.desc}</p>

                                <Link
                                    href={`/${locale}/menu?category=${c.key}`}
                                    className="mt-4 inline-flex items-center gap-2 font-semibold text-blue-700 hover:underline"
                                >
                                    {t("explore")} <span aria-hidden>→</span>
                                </Link>
                            </div>
                        ))}
                    </div>

                    <Link
                        href={`/${locale}/menu`}
                        className="inline-flex rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:hidden"
                    >
                        {t("seeAll")} →
                    </Link>
                </section>



                {/* ================= BEST SELLERS (APP-LIKE HORIZONTAL SCROLL) ================= */}
                <section className="space-y-5">
                    <div className="flex items-end justify-between gap-4">
                        <div>
                            <h2 className="text-2xl font-extrabold text-gray-900">{t("bestSellersTitle")}</h2>
                            <p className="mt-1 text-gray-600">{t("bestSellersSubtitle")}</p>
                        </div>
                        <Link
                            href={`/${locale}/menu`}
                            className="hidden rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:inline-flex"
                        >
                            {t("seeAll")} →
                        </Link>
                    </div>

                    {/* Horizontal scroll for mobile-first, app-like feel */}
                    <div className="-mx-4 overflow-x-auto px-4 pb-2">
                        <div className="flex min-w-max gap-4">
                            {[
                                { name: t("bs1Name"), desc: t("bs1Desc"), price: "5.20 €" },
                                { name: t("bs2Name"), desc: t("bs2Desc"), price: "4.50 €" },
                                { name: t("bs3Name"), desc: t("bs3Desc"), price: "6.90 €" }
                            ].map((p) => (
                                <div
                                    key={p.name}
                                    className="w-[280px] rounded-3xl border bg-white p-6 shadow-sm"
                                >
                                    <div className="flex items-start justify-between gap-3">
                                        <div>
                                            <div className="inline-flex rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                                                {t("bestSellerTag")}
                                            </div>
                                            <h3 className="mt-3 text-lg font-semibold text-gray-900">{p.name}</h3>
                                            <p className="mt-2 text-sm text-gray-600">{p.desc}</p>
                                        </div>
                                        <div className="rounded-full bg-blue-600 px-3 py-1 text-sm font-bold text-white">
                                            {p.price}
                                        </div>
                                    </div>

                                    <div className="mt-5 flex gap-3">
                                        <Link
                                            href={`/${locale}/menu`}
                                            className="inline-flex flex-1 items-center justify-center rounded-2xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
                                        >
                                            {t("addToCart")}
                                        </Link>
                                        <Link
                                            href={`/${locale}/menu`}
                                            className="inline-flex items-center justify-center rounded-2xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50"
                                        >
                                            {t("details")}
                                        </Link>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <Link
                        href={`/${locale}/menu`}
                        className="inline-flex rounded-xl border px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 md:hidden"
                    >
                        {t("seeAll")} →
                    </Link>
                </section>

                {/* ================= INFO STRIP ================= */}
                <section className="rounded-3xl border bg-white p-8 shadow-sm">
                    <div className="grid gap-6 md:grid-cols-3">
                        {[
                            { title: t("info1Title"), desc: t("info1Desc") },
                            { title: t("info2Title"), desc: t("info2Desc") },
                            { title: t("info3Title"), desc: t("info3Desc") }
                        ].map((x) => (
                            <div key={x.title} className="rounded-3xl bg-blue-50 p-5">
                                <div className="text-sm font-semibold text-blue-700">{x.title}</div>
                                <div className="mt-2 text-gray-700">{x.desc}</div>
                            </div>
                        ))}
                    </div>
                </section>

                {/* ================= FOOTER ================= */}
                <footer className="border-t pt-8 pb-12 text-sm text-gray-600">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                        <span>© {new Date().getFullYear()} {t("brand")}</span>

                        <div className="flex gap-6">
                            <Link href={`/${locale}/impressum`} className="hover:underline">
                                Impressum
                            </Link>
                            <Link href={`/${locale}/privacy`} className="hover:underline">
                                Privacy / Datenschutz
                            </Link>
                        </div>
                    </div>
                </footer>
            </main>

            {/* ================= STICKY BOTTOM CTA (MOBILE) ================= */}
            <div className="fixed inset-x-0 bottom-0 z-50 border-t bg-white/90 p-3 backdrop-blur md:hidden">
                <div className="mx-auto flex max-w-6xl items-center gap-3 px-4">
                    <Link
                        href={`/${locale}/menu`}
                        className="inline-flex flex-1 items-center justify-center rounded-2xl border px-4 py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50"
                    >
                        {t("ctaMenu")}
                    </Link>
                    <Link
                        href={`/${locale}/checkout`}
                        className="inline-flex flex-1 items-center justify-center rounded-2xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700"
                    >
                        {t("ctaOrder")}
                    </Link>
                </div>
            </div>
        </div>
    );
}
