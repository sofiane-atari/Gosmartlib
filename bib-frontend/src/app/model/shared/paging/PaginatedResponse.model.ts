import { PaginatedMeta } from "../paging/PaginatedMeta.model.ts";

export interface PaginatedResponse<T> {
  data: T[];
  meta: PaginatedMeta;
}