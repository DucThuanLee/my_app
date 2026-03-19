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
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const logout = useAuthStore((state) => state.logout);

  function handleLogout() {
    clearAccessToken();
    logout();
    router.push(`/${locale}`);
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