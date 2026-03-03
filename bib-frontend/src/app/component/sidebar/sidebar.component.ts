import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/RoleAuthorization/auth.service';
import { FormsModule } from '@angular/forms';
import { Role } from '../../model/auth.model';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [FormsModule,],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {

  public authService = inject(AuthService);

  public constructor(){

  }

  onRoleChange(newRole: string) {
    this.authService.switchRole(newRole as Role);
  }

}
