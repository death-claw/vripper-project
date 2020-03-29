import {TestBed} from '@angular/core/testing';

import {CtxtMenuService} from './ctxt-menu.service';

describe('CtxtMenuService', () => {
  let service: CtxtMenuService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CtxtMenuService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
