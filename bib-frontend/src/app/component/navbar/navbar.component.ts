import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/RoleAuthorization/auth.service';
import { FormsModule } from '@angular/forms';
import { Role } from '../../model/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [FormsModule,],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

  public authService = inject(AuthService);

  public constructor(){

  }

  onRoleChange(newRole: string) {
    this.authService.switchRole(newRole as Role);
  }

}
