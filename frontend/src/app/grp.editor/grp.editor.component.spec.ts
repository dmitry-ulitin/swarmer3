import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GrpEditorComponent } from './grp.editor.component';

describe('GrpEditorComponent', () => {
  let component: GrpEditorComponent;
  let fixture: ComponentFixture<GrpEditorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GrpEditorComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(GrpEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
