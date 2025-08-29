export interface Category {
  categoryID: number;
  categoryName: string;
  description?: string;
}

export interface ProductVariant {
  id: number;
  productName: string;
  description: string;
  quantity: number;
  price: number;
  newPrice?: number;
  date?: string;
  imageUrl: string;
  videoUrl?: string;
  videoPublicIds?: string;
  product?: Product;
  category?: Category;
  country?: Country;
}

export interface Product {
  id: string;
  productName: string;
  price: number;
  rate: number;
  description: string;
  quantity: number;
  imageUrl: string;
  videoPublicId?: string;
  likes?: boolean;
  releaseDate: string;
  country: Country;
  viewCount: number;
  categories: Category[];
  productVariants: ProductVariant[];
}

export interface Country {
  countryId: string;
  countryName: string;
}
