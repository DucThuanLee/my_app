"use client";

import {create} from "zustand";
import {persist} from "zustand/middleware";

type AuthState = {
  accessToken: string | null;
  isAuthenticated: boolean;
  setAccessToken: (token: string) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      isAuthenticated: false,

      setAccessToken: (token) =>
        set({
          accessToken: token,
          isAuthenticated: true
        }),

      logout: () =>
        set({
          accessToken: null,
          isAuthenticated: false
        })
    }),
    {
      name: "restaurant-auth"
    }
  )
);