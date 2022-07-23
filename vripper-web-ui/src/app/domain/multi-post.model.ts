export class MultiPostModel {
  constructor(
    public type: string,
    public link: string,
    public threadId: string,
    public postId: string,
    public removed: boolean,
    public total: number,
    public loading: boolean
  ) {
  }
}
