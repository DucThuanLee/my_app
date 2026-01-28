import type {NextConfig} from "next";
import createNextIntlPlugin from "next-intl/plugin";

// IMPORTANT: Point to the correct file in ROOT
const withNextIntl = createNextIntlPlugin("./i18n/request.ts");

const nextConfig: NextConfig = {
  reactCompiler: true,
  turbopack: {}
};

export default withNextIntl(nextConfig);