/**
 * Product type that matches Java backend DTO:
 * ProductResponse(id, name, description, price, category, bestSeller)
 */
export type Product = {
  id: string;
  name: string;
  description?: string | null;
  price: number;
  category: string; // server-driven
  bestSeller: boolean;
};

/**
 * Category is server-driven; we don't use a hardcoded union type.
 */
export type Category = string;