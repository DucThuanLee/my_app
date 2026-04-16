type Props = {
  title: string;
  value: string;
  subtitle?: string;
};

export default function AdminStatCard({title, value, subtitle}: Props) {
  return (
    <div className="rounded-3xl border bg-white p-6 shadow-sm">
      <div className="text-sm font-semibold text-gray-500">{title}</div>
      <div className="mt-2 text-3xl font-extrabold text-gray-900">{value}</div>
      {subtitle ? (
        <div className="mt-2 text-sm text-gray-600">{subtitle}</div>
      ) : null}
    </div>
  );
}