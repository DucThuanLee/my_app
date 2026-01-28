import {headers} from "next/headers";
import {redirect} from "next/navigation";

async function detectLocale() {
  const acceptLanguage = (await headers()).get("accept-language") ?? "";

  // ưu tiên English nếu browser dùng en
  if (acceptLanguage.toLowerCase().startsWith("en")) {
    return "en";
  }

  // fallback mặc định (chuẩn Đức)
  return "de";
}

export default function RootPage() {
  const locale = detectLocale();
  redirect(`/${locale}`);
}