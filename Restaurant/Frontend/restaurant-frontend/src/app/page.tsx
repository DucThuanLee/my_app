import {headers} from "next/headers";
import {redirect} from "next/navigation";

export default async function RootPage() {
  const h = await headers();
  const acceptLanguage = h.get("accept-language") ?? "";
  const primary = acceptLanguage.split(",")[0]?.trim().toLowerCase() ?? "";

  const locale = primary.startsWith("en") ? "en" : "de";
  redirect(`/${locale}`);
}
