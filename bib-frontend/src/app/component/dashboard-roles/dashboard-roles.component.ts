import { Component, Inject } from '@angular/core';
import { AuthService } from '../../services/RoleAuthorization/auth.service';

@Component({
  selector: 'app-dashboard-roles',
  standalone: true,
  imports: [],
  templateUrl: './dashboard-roles.component.html',
  styleUrl: './dashboard-roles.component.css'
})
export class DashboardRolesComponent {

  public constructor(public authService: AuthService){

  }

  ngOnInit(){

    if(!this.authService.currentUser()){
      this.authService.fetchCurrentUser();
    }
  }

  
}
