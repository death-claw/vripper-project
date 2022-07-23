import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HomeTabsService {

  private _index$: Subject<number> = new BehaviorSubject(0);

  get index(): Observable<number> {
    return this._index$.asObservable();
  }

  setIndex(count: number) {
    this._index$.next(count);
  }
}
