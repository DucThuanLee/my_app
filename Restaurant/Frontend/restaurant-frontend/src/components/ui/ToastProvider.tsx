"use client";

import {createContext, useContext, useState} from "react";
import {motion, AnimatePresence} from "framer-motion";

type ToastType = "success" | "error" | "info" | "warning";

type Toast = {
  id: number;
  message: string;
  type: ToastType;
};

const ToastContext = createContext({
  show: (msg: string, type?: ToastType) => {}
});

export function useToast() {
  return useContext(ToastContext);
}

export default function ToastProvider({children}: {children: React.ReactNode}) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  function show(message: string, type: ToastType = "info") {
    const id = Date.now();

    setToasts((prev) => [...prev, {id, message, type}]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  }

  function remove(id: number) {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }

  return (
    <ToastContext.Provider value={{show}}>
      {children}

      <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3">
        <AnimatePresence>
          {toasts.map((t) => (
            <motion.div
              key={t.id}
              initial={{opacity: 0, y: 20, scale: 0.95}}
              animate={{opacity: 1, y: 0, scale: 1}}
              exit={{opacity: 0, y: 10, scale: 0.95}}
              transition={{duration: 0.2}}
              className={`flex items-center gap-3 rounded-xl px-4 py-3 shadow-lg text-sm font-medium
                ${getToastStyle(t.type)}
              `}
            >
              <span className="text-lg">{getIcon(t.type)}</span>

              <span className="flex-1">{t.message}</span>

              <button
                onClick={() => remove(t.id)}
                className="text-white/70 hover:text-white"
              >
                ✕
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  );
}

// 🎨 styles
function getToastStyle(type: ToastType) {
  switch (type) {
    case "success":
      return "bg-green-600 text-white";
    case "error":
      return "bg-red-600 text-white";
    case "warning":
      return "bg-yellow-500 text-black";
    default:
      return "bg-gray-900 text-white";
  }
}

// 🔔 icons
function getIcon(type: ToastType) {
  switch (type) {
    case "success":
      return "✔";
    case "error":
      return "✖";
    case "warning":
      return "⚠";
    default:
      return "ℹ";
  }
}