export interface HttpErrorResponseError {
    error: Error;
}

interface Error {
    code: number;
    message: string;
    details?: string;
}