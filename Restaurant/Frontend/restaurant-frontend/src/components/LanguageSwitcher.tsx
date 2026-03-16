"use client";

import {Link, usePathname} from "@i18n/navigation";

type Props = {
  locale: string;
};

export default function LanguageSwitcher({locale}: Props) {
  const pathname = usePathname();

  return (
    <div className="hidden items-center gap-1 rounded-xl border bg-white p-1 md:flex">
      <Link
        href={pathname}
        locale="de"
        className={`rounded-lg px-2 py-1 text-xs font-semibold ${
          locale === "de"
            ? "bg-blue-50 text-blue-700"
            : "text-gray-600 hover:bg-gray-50"
        }`}
      >
        DE
      </Link>

      <Link
        href={pathname}
        locale="en"
        className={`rounded-lg px-2 py-1 text-xs font-semibold ${
          locale === "en"
            ? "bg-blue-50 text-blue-700"
            : "text-gray-600 hover:bg-gray-50"
        }`}
      >
        EN
      </Link>
    </div>
  );
}