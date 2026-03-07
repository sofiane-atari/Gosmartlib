export interface HttpErrorResponseError {
    error: Error;
}

interface Error {
    code: number;
    message: string;
    details?: ErrorDetail[];
}
interface ErrorDetail {
    field: string;
    message: string;
}