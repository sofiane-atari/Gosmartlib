import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

    private readonly smartschoolOAuthUrl = `${environment.apiUrl}/auth/initiate-login`;

    public initiateSmartschoolSSO(): void {
      window.location.href = this.smartschoolOAuthUrl;
    }
}
