import ProductCardSkeleton from "@/components/ProductCardSkeleton";

export default function Loading() {
  return (
    <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
      <section className="space-y-3">
        <div className="h-8 w-40 rounded bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
        <div className="h-4 w-72 rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
      </section>

      <div className="flex gap-2">
        {Array.from({length: 4}).map((_, i) => (
          <div
            key={i}
            className="h-10 w-24 rounded-xl bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]"
          />
        ))}
      </div>

      <div className="flex gap-2">
        <div className="h-11 flex-1 rounded-xl bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
        <div className="h-11 w-28 rounded-xl bg-gradient-to-r from-blue-200 via-blue-100 to-blue-200 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
      </div>

      <div className="h-4 w-24 rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />

      <section className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({length: 6}).map((_, index) => (
          <ProductCardSkeleton key={index} />
        ))}
      </section>
    </main>
  );
}