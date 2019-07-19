export class PostState {
  constructor(
    public type: string,
    public postId: string,
    public postCounter: string,
    public title: string,
    public progress: number,
    public status: string,
    public removed: boolean
  ) {}
}
