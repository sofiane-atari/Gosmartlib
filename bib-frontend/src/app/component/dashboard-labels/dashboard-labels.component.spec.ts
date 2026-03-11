import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardLabelsComponent } from './dashboard-labels.component';

describe('DashboardLabelsComponent', () => {
  let component: DashboardLabelsComponent;
  let fixture: ComponentFixture<DashboardLabelsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardLabelsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DashboardLabelsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
