"use client";

import type {ReactNode} from "react";
import {useEffect} from "react";
import {useParams, usePathname, useRouter} from "next/navigation";
import {useAuthStore} from "@/stores/auth-store";

type Props = {
  children: ReactNode;
};

export default function RequireAuth({children}: Props) {
  const router = useRouter();
  const pathname = usePathname();
  const {locale} = useParams<{locale: string}>();

  const hydrated = useAuthStore((state) => state.hydrated);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  useEffect(() => {
    if (!hydrated) return;

    if (!isAuthenticated) {
      const redirectPath = pathname.replace(`/${locale}`, "") || "/";
      router.replace(`/${locale}/login?redirect=${encodeURIComponent(redirectPath)}`);
    }
  }, [hydrated, isAuthenticated, router, pathname, locale]);

  if (!hydrated) {
    return (
      <main className="mx-auto max-w-6xl px-4 py-10">
        <div className="rounded-3xl border bg-white p-8 shadow-sm">
          <div className="h-7 w-40 animate-pulse rounded bg-gray-200" />
          <div className="mt-3 h-4 w-72 animate-pulse rounded bg-gray-100" />
          <div className="mt-8 grid gap-4 md:grid-cols-2">
            <div className="h-36 animate-pulse rounded-2xl bg-gray-100" />
            <div className="h-36 animate-pulse rounded-2xl bg-gray-100" />
          </div>
        </div>
      </main>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}