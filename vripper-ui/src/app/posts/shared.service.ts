import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

@Injectable()
export class SharedService {

    private expandedPost$ = new Subject<string>();

    get expandedPost(): Observable<string> {
        return this.expandedPost$.asObservable();
    }

    publishExpanded(postId: string) {
        this.expandedPost$.next(postId);
    }
}
