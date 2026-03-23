import { Component, inject } from '@angular/core';
import { FormBuilder } from '@angular/forms';

@Component({
  selector: 'app-create-loan',
  standalone: true,
  imports: [],
  templateUrl: './create-loan.component.html',
  styleUrl: './create-loan.component.css'
})
export class CreateLoanComponent {
  private fb =  inject(FormBuilder);

  protected loanForm = this.fb.group({
    LoanerFirstName : [''],
    LoanerLastName : [''],
    Book: ['']
  });
}
