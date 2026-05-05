import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExtendLoanComponent } from './extend-loan.component';

describe('ExtendLoanComponent', () => {
  let component: ExtendLoanComponent;
  let fixture: ComponentFixture<ExtendLoanComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExtendLoanComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExtendLoanComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
