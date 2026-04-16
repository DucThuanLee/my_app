"use client";

import {useEffect, useState} from "react";
import {useRouter, usePathname} from "next/navigation";

import {getAccessToken} from "@/lib/auth-storage";
import {isAdmin} from "@/lib/auth";

export default function AdminGuard({children}: {children: React.ReactNode}) {
  const router = useRouter();
  const pathname = usePathname();
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    const token = getAccessToken();

    // ❌ Not logged in
    if (!token) {
      router.replace(`/en/login?redirect=${encodeURIComponent(pathname)}`);
      return;
    }

    // ❌ not an admin
    if (!isAdmin()) {
      router.replace("/en");
      return;
    }

    // ✅ ok
    setAllowed(true);
  }, [router, pathname]);

  if (!allowed) {
    return (
      <div className="flex h-[60vh] items-center justify-center text-gray-500">
        Checking permissions...
      </div>
    );
  }

  return <>{children}</>;
}