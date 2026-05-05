import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CampusReportComponent } from './campus-report.component';

describe('CampusReportComponent', () => {
  let component: CampusReportComponent;
  let fixture: ComponentFixture<CampusReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CampusReportComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CampusReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

