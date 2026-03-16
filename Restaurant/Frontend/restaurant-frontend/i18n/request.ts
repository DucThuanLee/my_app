import {getRequestConfig} from "next-intl/server";
import {notFound} from "next/navigation";

const locales = ["de", "en"] as const;
const defaultLocale = "de";

function normalizeLocale(locale?: string) {
  const value = (locale ?? "").trim();
  if (!value) return defaultLocale;
  return value.split("-")[0];
}

export default getRequestConfig(async ({requestLocale}) => {
  const requested = await requestLocale;
  const locale = normalizeLocale(requested);

  if (!locales.includes(locale as (typeof locales)[number])) {
    notFound();
  }

  return {
    locale,
    messages:
      locale === "de"
        ? (await import("../messages/de.json")).default
        : (await import("../messages/en.json")).default
  };
});