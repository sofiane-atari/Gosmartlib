import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { 
  LucideAngularModule, 
  Book, Heart, History, Star, UserCircle, Settings, 
  Package, Flag, FileText, Tag, Plus, RotateCcw, LogOut, User 
} from 'lucide-angular';
import { School } from 'lucide-angular/src/icons';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
activeMenu = 0;

  readonly PlusIcon = Plus;
  readonly UserIcon = User;
  readonly RotateCcwIcon = RotateCcw;
  readonly LogOutIcon = LogOut;

menuItems = [
    { icon: Book, label: 'Catalogus' },
    { icon: Heart, label: 'Favorietenlijst' },
    { icon: History, label: 'Geschiedenis' },
    { icon: Star, label: 'Mijn Reviews' },
    { icon: UserCircle, label: 'Mijn Account' },
    { icon: Settings, label: 'Instellingen' },
    { icon: Package, label: 'BookBox' },
    { icon: Flag, label: 'Rapporteer' },
    { icon: FileText, label: 'Klasrapportages' },
    { icon: Tag, label: 'Labelbeheer' },
    { icon: School, label: 'Schoolbeheer' },
  ];

  setActive(index: number): void {
    this.activeMenu = index;
  }
}
