"use client";

import Link from "next/link";
import {useEffect, useRef, useState} from "react";
import {useRouter} from "next/navigation";

import {useAuthStore} from "@/stores/auth-store";
import {clearAccessToken} from "@/lib/auth-storage";

type Props = {
  locale: string;
  loginLabel: string;
  registerLabel: string;
  accountLabel: string;
  ordersLabel: string;
  logoutLabel: string;
};

export default function HeaderAuthActions({
  locale,
  loginLabel,
  registerLabel,
  accountLabel,
  ordersLabel,
  logoutLabel
}: Props) {
  const router = useRouter();
  const desktopDropdownRef = useRef<HTMLDivElement | null>(null);
  const mobileDropdownRef = useRef<HTMLDivElement | null>(null);

  const hydrated = useAuthStore((state) => state.hydrated);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const logout = useAuthStore((state) => state.logout);

  const [desktopOpen, setDesktopOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        desktopDropdownRef.current &&
        !desktopDropdownRef.current.contains(event.target as Node)
      ) {
        setDesktopOpen(false);
      }

      if (
        mobileDropdownRef.current &&
        !mobileDropdownRef.current.contains(event.target as Node)
      ) {
        setMobileOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setDesktopOpen(false);
        setMobileOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  function handleLogout() {
    clearAccessToken();
    logout();
    setDesktopOpen(false);
    setMobileOpen(false);
    router.push(`/${locale}`);
    router.refresh();
  }

  if (!hydrated) {
    return (
      <>
        <div className="hidden items-center gap-2 md:flex">
          <div className="h-10 w-20 rounded-xl bg-gray-100" />
          <div className="h-10 w-24 rounded-xl bg-gray-100" />
        </div>

        <div className="md:hidden">
          <div className="h-10 w-10 rounded-xl bg-gray-100" />
        </div>
      </>
    );
  }

  if (!isAuthenticated) {
    return (
      <>
        <div className="hidden items-center gap-2 md:flex">
          <Link
            href={`/${locale}/login`}
            className="rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50"
          >
            {loginLabel}
          </Link>

          <Link
            href={`/${locale}/register`}
            className="rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            {registerLabel}
          </Link>
        </div>

        <div ref={mobileDropdownRef} className="relative md:hidden">
          <button
            type="button"
            onClick={() => setMobileOpen((prev) => !prev)}
            className="inline-flex h-10 w-10 items-center justify-center rounded-xl border bg-white text-gray-700 hover:bg-gray-50"
            aria-expanded={mobileOpen}
            aria-haspopup="menu"
            aria-label="Open authentication menu"
          >
            ☰
          </button>

          {mobileOpen ? (
            <div className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-48 rounded-2xl border bg-white p-2 shadow-xl">
              <Link
                href={`/${locale}/login`}
                onClick={() => setMobileOpen(false)}
                className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-blue-50"
              >
                {loginLabel}
              </Link>

              <Link
                href={`/${locale}/register`}
                onClick={() => setMobileOpen(false)}
                className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50"
              >
                {registerLabel}
              </Link>
            </div>
          ) : null}
        </div>
      </>
    );
  }

  return (
    <>
      <div ref={desktopDropdownRef} className="relative hidden md:block">
        <button
          type="button"
          onClick={() => setDesktopOpen((prev) => !prev)}
          className="inline-flex items-center gap-2 rounded-xl border bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          aria-expanded={desktopOpen}
          aria-haspopup="menu"
        >
          <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
            A
          </span>
          <span>{accountLabel}</span>
          <span className="text-xs text-gray-400">▾</span>
        </button>

        {desktopOpen ? (
          <div className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-56 rounded-2xl border bg-white p-2 shadow-xl">
            <Link
              href={`/${locale}/account`}
              onClick={() => setDesktopOpen(false)}
              className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {accountLabel}
            </Link>

            <Link
              href={`/${locale}/orders`}
              onClick={() => setDesktopOpen(false)}
              className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {ordersLabel}
            </Link>

            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full items-center rounded-xl px-4 py-3 text-left text-sm font-semibold text-red-600 hover:bg-red-50"
            >
              {logoutLabel}
            </button>
          </div>
        ) : null}
      </div>

      <div ref={mobileDropdownRef} className="relative md:hidden">
        <button
          type="button"
          onClick={() => setMobileOpen((prev) => !prev)}
          className="inline-flex h-10 w-10 items-center justify-center rounded-xl border bg-white text-gray-700 hover:bg-gray-50"
          aria-expanded={mobileOpen}
          aria-haspopup="menu"
          aria-label="Open account menu"
        >
          <span className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-blue-600 text-[10px] font-bold text-white">
            A
          </span>
        </button>

        {mobileOpen ? (
          <div className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-52 rounded-2xl border bg-white p-2 shadow-xl">
            <Link
              href={`/${locale}/account`}
              onClick={() => setMobileOpen(false)}
              className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {accountLabel}
            </Link>

            <Link
              href={`/${locale}/orders`}
              onClick={() => setMobileOpen(false)}
              className="flex w-full items-center rounded-xl px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-blue-50"
            >
              {ordersLabel}
            </Link>

            <button
              type="button"
              onClick={handleLogout}
              className="flex w-full items-center rounded-xl px-4 py-3 text-left text-sm font-semibold text-red-600 hover:bg-red-50"
            >
              {logoutLabel}
            </button>
          </div>
        ) : null}
      </div>
    </>
  );
}