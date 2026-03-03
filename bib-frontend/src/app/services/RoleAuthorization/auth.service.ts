import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthUserDto, Role } from '../../model/auth.model';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);
  
  // Signal om de huidige gebruiker globaal beschikbaar te maken
  public currentUser = signal<AuthUserDto | null>(null);
  
  // De geselecteerde mock rol (om mee te sturen naar de backend)
  public activeMockRole = signal<Role>('STUDENT'); 

  // Haal de gebruiker op van de backend
  public fetchCurrentUser() {
    this.http.get<AuthUserDto>('http://localhost:8080/api/auth/me').subscribe({
      next: (user) => this.currentUser.set(user),
      error: (err) => console.error('Fout bij ophalen gebruiker', err)
    });
  }

  // Methode om van rol te wisselen in de UI
  public switchRole(role: Role) {
    this.activeMockRole.set(role);
    this.fetchCurrentUser();
  }

  public hasRole(role: Role): boolean {
    return this.currentUser()?.role === role;
  }

  
}
