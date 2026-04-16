"use client";

import {
  ResponsiveContainer,
  AreaChart,
  Area,
  CartesianGrid,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";

type Point = {
  date: string;
  revenue: number;
};

type Props = {
  data: Point[];
};

export default function AdminRevenueChart({data}: Props) {
  return (
    <div className="rounded-3xl border bg-white p-6 shadow-sm">
      <div className="mb-4">
        <h2 className="text-xl font-extrabold text-gray-900">Revenue (7 days)</h2>
        <p className="mt-1 text-sm text-gray-600">
          Based on paid orders in the loaded admin dataset.
        </p>
      </div>

      <div className="h-80">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Area type="monotone" dataKey="revenue" strokeWidth={2} fillOpacity={0.15} />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}