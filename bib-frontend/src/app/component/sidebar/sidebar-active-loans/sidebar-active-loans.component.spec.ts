import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SidebarActiveLoansComponent } from './sidebar-active-loans.component';

describe('SidebarActiveLoansComponent', () => {
  let component: SidebarActiveLoansComponent;
  let fixture: ComponentFixture<SidebarActiveLoansComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarActiveLoansComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SidebarActiveLoansComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
