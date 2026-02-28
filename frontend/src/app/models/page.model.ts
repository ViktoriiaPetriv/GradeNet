export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
