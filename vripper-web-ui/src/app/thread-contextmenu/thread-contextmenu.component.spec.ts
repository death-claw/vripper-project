import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThreadContextmenuComponent } from './thread-contextmenu.component';

describe('ThreadContextmenuComponent', () => {
  let component: ThreadContextmenuComponent;
  let fixture: ComponentFixture<ThreadContextmenuComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ThreadContextmenuComponent]
    });
    fixture = TestBed.createComponent(ThreadContextmenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
