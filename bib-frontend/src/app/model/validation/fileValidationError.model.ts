export interface FileValidationError {
  row: number;
  field: string;
  message: string;
  severity: 'error' | 'warning' | 'info';
}