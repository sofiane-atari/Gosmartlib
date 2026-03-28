import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

    private readonly smartschoolOAuthUrl = `${environment.apiUrl}/oauth2/authorization/smartschool`;

    public initiateSmartschoolSSO(): void {
      window.location.href = this.smartschoolOAuthUrl;
    }
}
