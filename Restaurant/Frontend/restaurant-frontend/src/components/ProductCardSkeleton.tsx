export default function ProductCardSkeleton() {
    return (
      <article className="rounded-3xl border bg-white p-6 shadow-sm">
        <div className="mb-4 h-36 rounded-2xl bg-gradient-to-r from-blue-50 via-blue-100 to-blue-50 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
  
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <div className="mb-2 h-6 w-24 rounded-full bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
  
            <div className="h-5 w-2/3 rounded bg-gradient-to-r from-gray-200 via-gray-100 to-gray-200 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
  
            <div className="mt-3 h-4 w-full rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
            <div className="mt-2 h-4 w-4/5 rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
          </div>
  
          <div className="h-8 w-20 shrink-0 rounded-full bg-gradient-to-r from-blue-200 via-blue-100 to-blue-200 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
        </div>
  
        <div className="mt-4 h-4 w-24 rounded bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
  
        <div className="mt-5 flex gap-3">
          <div className="h-10 flex-1 rounded-2xl bg-gradient-to-r from-blue-200 via-blue-100 to-blue-200 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
          <div className="h-10 w-24 rounded-2xl bg-gradient-to-r from-gray-100 via-gray-200 to-gray-100 bg-[length:200%_100%] animate-[shimmer_1.6s_infinite]" />
        </div>
      </article>
    );
  }