import type {Metadata} from "next";
import {Geist, Geist_Mono} from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"]
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"]
});

export const metadata: Metadata = {
  title: "Milk Tea Restaurant",
  description: "Order milk tea, coffee and snacks online"
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html>
      <body
        className={`${geistSans.variable} ${geistMono.variable} min-h-screen bg-gradient-to-b from-blue-50/60 via-white to-white text-gray-900 antialiased`}
      >
        {children}
      </body>
    </html>
  );
}