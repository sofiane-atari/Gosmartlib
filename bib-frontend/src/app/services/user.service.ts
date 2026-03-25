import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams} from '@angular/common/http';
import { User } from '../model/user.model';
import { environment } from '../../environments/environment';
import { AuthUserDto } from '../model/auth.model';

export interface GetUsersOpt {
    WithfirstName?: string;
    WithlastName?: string; 
    WithId?: String;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private http = inject(HttpClient);   
  private apiUrl = environment.apiUrl;

  public getUsers(opt?: GetUsersOpt): Observable<AuthUserDto[]> {
    let params = new HttpParams({
      fromObject: {
        firstName: String(opt?.WithfirstName?? ''),
        lastName: String(opt?.WithlastName ?? ''),
        id: String(opt?.WithId ?? ''),
      },
    });

    return this.http.get<AuthUserDto[]>(`${this.apiUrl}/users`, { params: params });
  }
}