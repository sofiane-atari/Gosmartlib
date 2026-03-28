import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardGenresComponent } from './dashboard-genres.component';

describe('DashboardGenresComponent', () => {
  let component: DashboardGenresComponent;
  let fixture: ComponentFixture<DashboardGenresComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardGenresComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardGenresComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
