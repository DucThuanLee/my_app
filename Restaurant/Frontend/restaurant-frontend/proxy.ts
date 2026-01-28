import createMiddleware from "next-intl/middleware";

export default createMiddleware({
 // A list of all locales that are supported
 locales: ['de', 'en'],
  
 // Default locale (German for Germany)
 defaultLocale: 'de',
 
 // Automatically detect locale from browser
 localeDetection: true
});

export const config = {
  // Match all pathnames except for
  // - /api (API routes)
  // - /_next (Next.js internals)
  // - Static files (images, favicon, etc.)
  matcher: ["/((?!api|_next|.*\\..*).*)"]
};

// import createMiddleware from "next-intl/middleware";

// export default createMiddleware({
//   locales: ["de", "en"],
//   defaultLocale: "de"
// });

// export const config = {
//   matcher: ["/", "/(de|en)/:path*"]
// };
