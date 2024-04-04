import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccEditorComponent } from './acc.editor.component';

describe('AccEditorComponent', () => {
  let component: AccEditorComponent;
  let fixture: ComponentFixture<AccEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccEditorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AccEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
