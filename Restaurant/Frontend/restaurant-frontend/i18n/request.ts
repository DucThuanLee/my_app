import {getRequestConfig} from "next-intl/server";
import {notFound} from "next/navigation";

/**
 * Normalizes locales like "de-DE" -> "de".
 * Also handles undefined/empty locale safely.
 */
function normalizeLocale(locale?: string) {
  const value = (locale ?? "").trim();
  if (!value) return "de";
  return value.split("-")[0];
}

export default getRequestConfig(async ({locale}) => {
  const normalized = normalizeLocale(locale);

  // Only allow the locales we support
  if (normalized !== "de" && normalized !== "en") notFound();

  return {
    locale: normalized,
    messages:
      normalized === "de"
        ? (await import("../messages/de.json")).default
        : (await import("../messages/en.json")).default
  };
});
