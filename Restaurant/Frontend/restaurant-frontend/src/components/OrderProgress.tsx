import {OrderStatus} from "@/types/order";

type Props = {
  status: OrderStatus;
  newLabel: string;
  preparingLabel: string;
  doneLabel: string;
};

const steps: OrderStatus[] = [
  OrderStatus.NEW,
  OrderStatus.PREPARING,
  OrderStatus.DONE
];

function getStepIndex(status: OrderStatus) {
  switch (status) {
    case OrderStatus.NEW:
      return 0;
    case OrderStatus.PREPARING:
      return 1;
    case OrderStatus.DONE:
      return 2;
    case OrderStatus.CANCELLED:
      return -1;
    default:
      return -1;
  }
}

export default function OrderProgress({
  status,
  newLabel,
  preparingLabel,
  doneLabel
}: Props) {
  const labels = [newLabel, preparingLabel, doneLabel];
  const activeIndex = getStepIndex(status);

  if (status === OrderStatus.CANCELLED) {
    return (
      <div className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
        Cancelled
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-3 gap-2">
        {steps.map((step, index) => {
          const done = activeIndex >= index;
          return (
            <div key={step} className="flex flex-col items-center gap-2">
              <div
                className={`h-3 w-full rounded-full ${
                  done ? "bg-blue-600" : "bg-gray-200"
                }`}
              />
              <span
                className={`text-xs font-semibold ${
                  done ? "text-blue-700" : "text-gray-500"
                }`}
              >
                {labels[index]}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}