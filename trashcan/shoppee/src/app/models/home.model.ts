import { Category, Product, ProductVariant } from './product.model';

export interface HomeResponse {
  loggedUser: string;
  totalMovies: number;
  totalCategories: number;
  totalUsers: number;
  loggedInUser: any;
  category: Category[];
  products: Product[];
  price: Product[];
  score: Product[];
  release: Product[];
}