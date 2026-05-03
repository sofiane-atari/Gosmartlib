import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  BookOpen,
  RotateCcw,
  LibraryBig,
  Clock,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-angular';

@Component({
  selector: 'app-book-management',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './book-management.component.html',
  styleUrl: './book-management.component.css',
})
export class BookManagementComponent {
  protected readonly BookOpen = BookOpen;
  protected readonly RotateCcw = RotateCcw;
  protected readonly LibraryBig = LibraryBig;
  protected readonly Clock = Clock;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;

  protected readonly loanStats = {
    active: 0,
    overdue: 0,
    returned: 0,
  };
}
