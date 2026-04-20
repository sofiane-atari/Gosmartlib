import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialOverviewComponent } from './didactic-material-overview.component';

describe('DidacticMaterialOverviewComponent', () => {
  let component: DidacticMaterialOverviewComponent;
  let fixture: ComponentFixture<DidacticMaterialOverviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialOverviewComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialOverviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
