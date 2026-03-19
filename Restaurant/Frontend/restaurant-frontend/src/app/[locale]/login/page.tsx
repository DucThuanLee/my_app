"use client";

import Link from "next/link";
import {useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {useTranslations} from "next-intl";
import AuthShell from "@/components/AuthShell";
import {login} from "@/lib/auth-api";
import {saveAccessToken} from "@/lib/auth-storage";
import {useAuthStore} from "@/stores/auth-store";

export default function LoginPage() {
  const {locale} = useParams<{locale: string}>();
  const router = useRouter();
  const t = useTranslations("auth");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const setAccessToken = useAuthStore((state) => state.setAccessToken);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErrorMessage("");
  
    if (!email.trim() || !password.trim()) {
      setErrorMessage(t("errors.required"));
      return;
    }
  
    try {
      setSubmitting(true);
  
      const result = await login({
        email: email.trim(),
        password
      });
  
      saveAccessToken(result.accessToken);
      setAccessToken(result.accessToken);
  
      router.push(`/${locale}`);
    } catch (error) {
      console.error(error);
      setErrorMessage(t("errors.loginFailed"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell
      locale={locale}
      badge={t("login.badge")}
      title={t("login.title")}
      subtitle={t("login.subtitle")}
      footerText={t("login.footerText")}
      footerLinkLabel={t("login.footerLink")}
      footerHref="/register"
    >
      <div className="space-y-2">
        <h2 className="text-2xl font-extrabold text-gray-900">
          {t("login.formTitle")}
        </h2>
        <p className="text-sm text-gray-600">{t("login.formSubtitle")}</p>
      </div>

      <form onSubmit={handleSubmit} className="mt-6 space-y-4">
        <div>
          <label className="mb-2 block text-sm font-semibold text-gray-700">
            {t("fields.email")}
          </label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t("placeholders.email")}
            className="w-full rounded-2xl border px-4 py-3 outline-none ring-blue-200 focus:ring-4"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold text-gray-700">
            {t("fields.password")}
          </label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder={t("placeholders.password")}
            className="w-full rounded-2xl border px-4 py-3 outline-none ring-blue-200 focus:ring-4"
          />
        </div>

        <div className="flex items-center justify-between gap-4 text-sm">
          <label className="inline-flex items-center gap-2 text-gray-600">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
            />
            <span>{t("login.rememberMe")}</span>
          </label>

          <Link
            href={`/${locale}/forgot-password`}
            className="font-semibold text-blue-700 hover:underline"
          >
            {t("login.forgotPassword")}
          </Link>
        </div>

        {errorMessage ? (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {errorMessage}
          </div>
        ) : null}

        <button
          type="submit"
          disabled={submitting}
          className="inline-flex w-full items-center justify-center rounded-2xl bg-blue-600 px-6 py-3 font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {submitting ? t("login.signingIn") : t("login.submit")}
        </button>
      </form>
    </AuthShell>
  );
}