/**
 * Build backend URL safely from NEXT_PUBLIC_API_URL.
 */
export function backendUrl(path: string) {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL;
  
    if (!baseUrl) {
      throw new Error("NEXT_PUBLIC_API_URL is not defined in .env.local");
    }
  
    return new URL(path, baseUrl);
  }
  
  /**
   * Parse JSON response or throw a readable error.
   */
  export async function readJsonOrThrow<T>(res: Response, message: string): Promise<T> {
    if (!res.ok) {
      throw new Error(`${message}: ${res.status} ${res.statusText}`);
    }
  
    return res.json() as Promise<T>;
  }
  
  /**
   * Format a number to EUR currency.
   */
  export function formatPriceEUR(price: number) {
    return new Intl.NumberFormat("de-DE", {
      style: "currency",
      currency: "EUR"
    }).format(price);
  }