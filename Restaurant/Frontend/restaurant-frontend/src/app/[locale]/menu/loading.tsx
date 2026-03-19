export default function Loading() {
    return (
      <main className="mx-auto max-w-6xl space-y-8 px-4 pb-24 pt-8">
        <section className="space-y-3">
          <div className="h-8 w-40 animate-pulse rounded bg-gray-200" />
          <div className="h-4 w-72 animate-pulse rounded bg-gray-100" />
        </section>
  
        <div className="flex gap-2">
          {Array.from({length: 4}).map((_, i) => (
            <div key={i} className="h-10 w-24 animate-pulse rounded-xl bg-gray-100" />
          ))}
        </div>
  
        <div className="flex gap-2">
          <div className="h-11 flex-1 animate-pulse rounded-xl bg-gray-100" />
          <div className="h-11 w-28 animate-pulse rounded-xl bg-blue-100" />
        </div>
  
        <div className="h-4 w-24 animate-pulse rounded bg-gray-100" />
  
        <section className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({length: 6}).map((_, index) => (
            <article key={index} className="rounded-3xl border bg-white p-6 shadow-sm">
              <div className="mb-4 h-36 animate-pulse rounded-2xl bg-blue-50" />
              <div className="h-5 w-2/3 animate-pulse rounded bg-gray-200" />
              <div className="mt-3 h-4 w-full animate-pulse rounded bg-gray-100" />
              <div className="mt-2 h-4 w-4/5 animate-pulse rounded bg-gray-100" />
              <div className="mt-5 h-10 animate-pulse rounded-2xl bg-blue-100" />
            </article>
          ))}
        </section>
      </main>
    );
  }