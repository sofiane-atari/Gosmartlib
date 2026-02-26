import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  LucideAngularModule, 
  Book, Heart, History, Star, UserCircle, Settings, 
  Package, Flag, FileText, Tag, Plus, RotateCcw, LogOut, User 
} from 'lucide-angular';
import { School } from 'lucide-angular/src/icons';
import { RouterModule } from '@angular/router';
import { routes } from '../../app.routes';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {

  readonly PlusIcon = Plus;
  readonly UserIcon = User;
  readonly RotateCcwIcon = RotateCcw;
  readonly LogOutIcon = LogOut;

menuItems = [
    { icon: Book, label: 'Catalogus', route: '/catalogus' },
    { icon: Heart, label: 'Favorietenlijst', route: '/favorieten' },
    { icon: History, label: 'Geschiedenis', route: '/historiek' },
    { icon: Star, label: 'Mijn Reviews', route: '/reviews' },
    { icon: UserCircle, label: 'Mijn Account', route: '/account' },
    { icon: Settings, label: 'Instellingen', route: '/instellingen' },
    { icon: Package, label: 'BookBox', route: '/bookbox' },
    { icon: Flag, label: 'Rapporteer', route: '/gerapporteerd' },
    { icon: FileText, label: 'Klasrapportages', route: '/klasraportage' },
    { icon: Tag, label: 'Labelbeheer', route: '/labels' },
    { icon: School, label: 'Schoolbeheer', route: '/scholen' },
  ];
}
