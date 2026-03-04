import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardRolesComponent } from './dashboard-roles.component';

describe('DashboardRolesComponent', () => {
  let component: DashboardRolesComponent;
  let fixture: ComponentFixture<DashboardRolesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardRolesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardRolesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
