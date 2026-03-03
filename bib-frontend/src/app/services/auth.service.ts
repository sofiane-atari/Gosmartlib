import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface User {
    id: number;
    displayName: string;
    smartschoolId: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    // Hardcoded dev user for local development
    private currentUserSubject = new BehaviorSubject<User | null>({
        id: 1,
        displayName: 'Dev Gebruiker',
        smartschoolId: 'dev-user'
    });

    constructor() { }

    getCurrentUser(): Observable<User | null> {
        return this.currentUserSubject.asObservable();
    }

    getCurrentUserId(): number | null {
        const user = this.currentUserSubject.value;
        return user ? user.id : null;
    }
}
