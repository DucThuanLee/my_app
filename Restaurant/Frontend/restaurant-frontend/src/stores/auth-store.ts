"use client";

import {create} from "zustand";
import {persist} from "zustand/middleware";

type AuthState = {
  accessToken: string | null;
  isAuthenticated: boolean;
  hydrated: boolean;
  setAccessToken: (token: string) => void;
  logout: () => void;
  setHydrated: (value: boolean) => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      isAuthenticated: false,
      hydrated: false,

      setAccessToken: (token) =>
        set({
          accessToken: token,
          isAuthenticated: true
        }),

      logout: () =>
        set({
          accessToken: null,
          isAuthenticated: false
        }),

      setHydrated: (value) =>
        set({
          hydrated: value
        })
    }),
    {
      name: "restaurant-auth",
      onRehydrateStorage: () => (state) => {
        state?.setHydrated(true);
      }
    }
  )
);