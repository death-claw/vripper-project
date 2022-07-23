import {SelectionService} from '../services/selection-service';
import {ChangeDetectionStrategy, Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {WsConnectionService} from '../services/ws-connection.service';
import {BehaviorSubject, Subject, Subscription} from 'rxjs';
import {GlobalState} from '../domain/global-state.model';

@Component({
  selector: 'app-status-bar',
  templateUrl: './status-bar.component.html',
  styleUrls: ['./status-bar.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBarComponent implements OnInit, OnDestroy {
  globalState$: Subject<GlobalState> = new BehaviorSubject(new GlobalState(0, 0, 0, '', ''));
  selected$: Subject<number> = new BehaviorSubject(0);
  subscriptions: Subscription[] = [];

  constructor(private ws: WsConnectionService, private ngZone: NgZone, private selectionService: SelectionService) {
  }

  ngOnInit() {
    this.subscriptions.push(this.ws.globalState$.subscribe(e => this.ngZone.run(() => this.globalState$.next(e))));
    this.subscriptions.push(this.selectionService.selected$.subscribe(selected => this.ngZone.run(() => this.selected$.next(selected.length))));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(e => e.unsubscribe());
  }
}
