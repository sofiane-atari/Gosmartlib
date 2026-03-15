import { FileValidationError } from "./FileValidationError";

export interface FileValidationResult {
  totalRows: number;
  validRows: number;
  invalidRows: number;
  errors: FileValidationError[];
}