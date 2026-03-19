"use client";

import {useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {useTranslations} from "next-intl";
import {useAuthStore} from "@/stores/auth-store";
import AuthShell from "@/components/AuthShell";
import { saveAccessToken } from "@/lib/auth-storage";
import { register } from "@/lib/auth-api";

export default function RegisterPage() {
  const {locale} = useParams<{locale: string}>();
  const router = useRouter();
  const t = useTranslations("auth");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [acceptTerms, setAcceptTerms] = useState(false);
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
  
    if (password.length < 8) {
      setErrorMessage(t("errors.passwordTooShort"));
      return;
    }
  
    if (password !== confirmPassword) {
      setErrorMessage(t("errors.passwordsDoNotMatch"));
      return;
    }
  
    if (!acceptTerms) {
      setErrorMessage(t("errors.acceptTerms"));
      return;
    }
  
    try {
      setSubmitting(true);
  
      const result = await register({
        email: email.trim(),
        password
      });
  
      saveAccessToken(result.accessToken);
      setAccessToken(result.accessToken);
  
      router.push(`/${locale}`);
    } catch (error) {
      console.error(error);
      setErrorMessage(t("errors.registerFailed"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthShell
      locale={locale}
      badge={t("register.badge")}
      title={t("register.title")}
      subtitle={t("register.subtitle")}
      footerText={t("register.footerText")}
      footerLinkLabel={t("register.footerLink")}
      footerHref="/login"
    >
      <div className="space-y-2">
        <h2 className="text-2xl font-extrabold text-gray-900">
          {t("register.formTitle")}
        </h2>
        <p className="text-sm text-gray-600">{t("register.formSubtitle")}</p>
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

        <div>
          <label className="mb-2 block text-sm font-semibold text-gray-700">
            {t("fields.confirmPassword")}
          </label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder={t("placeholders.confirmPassword")}
            className="w-full rounded-2xl border px-4 py-3 outline-none ring-blue-200 focus:ring-4"
          />
        </div>

        <label className="inline-flex items-start gap-3 text-sm text-gray-600">
          <input
            type="checkbox"
            className="mt-1"
            checked={acceptTerms}
            onChange={(e) => setAcceptTerms(e.target.checked)}
          />
          <span>{t("register.acceptTerms")}</span>
        </label>

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
          {submitting ? t("register.creating") : t("register.submit")}
        </button>
      </form>
    </AuthShell>
  );
}