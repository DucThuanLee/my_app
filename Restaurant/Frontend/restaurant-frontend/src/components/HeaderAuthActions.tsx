"use client";

import Link from "next/link";
import {useRouter} from "next/navigation";
import {useAuthStore} from "@/stores/auth-store";
import {clearAccessToken} from "@/lib/auth-storage";

type Props = {
  locale: string;
  loginLabel: string;
  registerLabel: string;
  logoutLabel: string;
  accountLabel: string;
};

export default function HeaderAuthActions({
  locale,
  loginLabel,
  registerLabel,
  logoutLabel,
  accountLabel
}: Props) {
  const router = useRouter();

  const hydrated = useAuthStore((state) => state.hydrated);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const logout = useAuthStore((state) => state.logout);
  console.log("auth hydrated", hydrated, "isAuthenticated", isAuthenticated);
  function handleLogout() {
    clearAccessToken();
    logout();
    router.push(`/${locale}`);
    router.refresh();
  }
  if (!hydrated) {
    return (
      <div className="hidden md:flex items-center gap-2">
        <div className="h-10 w-20 rounded-xl bg-gray-100" />
        <div className="h-10 w-24 rounded-xl bg-gray-100" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <>
        <Link
          href={`/${locale}/login`}
          className="hidden rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50 md:inline-flex"
        >
          {loginLabel}
        </Link>

        <Link
          href={`/${locale}/register`}
          className="hidden rounded-xl bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 md:inline-flex"
        >
          {registerLabel}
        </Link>
      </>
    );
  }

  return (
    <>
      <Link
        href={`/${locale}/account`}
        className="hidden rounded-xl px-3 py-2 text-sm font-semibold text-gray-700 hover:bg-blue-50 md:inline-flex"
      >
        {accountLabel}
      </Link>

      <button
        type="button"
        onClick={handleLogout}
        className="hidden rounded-xl border px-4 py-2 text-sm font-semibold text-gray-700 hover:bg-gray-50 md:inline-flex"
      >
        {logoutLabel}
      </button>
    </>
  );
}