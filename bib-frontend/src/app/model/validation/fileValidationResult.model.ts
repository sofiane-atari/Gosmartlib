import { FileValidationError } from "./fileValidationError.model";

export interface FileValidationResult {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: FileValidationError[];
}