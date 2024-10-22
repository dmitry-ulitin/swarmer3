import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RuleEditorComponent } from './rule.editor.component';

describe('RuleComponent', () => {
  let component: RuleEditorComponent;
  let fixture: ComponentFixture<RuleEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuleEditorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(RuleEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
