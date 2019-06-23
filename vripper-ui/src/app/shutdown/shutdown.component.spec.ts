import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ShutdownComponent } from './shutdown.component';

describe('ShutdownComponent', () => {
  let component: ShutdownComponent;
  let fixture: ComponentFixture<ShutdownComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ShutdownComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ShutdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
