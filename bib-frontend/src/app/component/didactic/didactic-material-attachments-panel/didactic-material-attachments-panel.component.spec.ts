import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DidacticMaterialAttachmentsPanelComponent } from './didactic-material-attachments-panel.component';

describe('DidacticMaterialAttachmentsPanelComponent', () => {
  let component: DidacticMaterialAttachmentsPanelComponent;
  let fixture: ComponentFixture<DidacticMaterialAttachmentsPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DidacticMaterialAttachmentsPanelComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DidacticMaterialAttachmentsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
