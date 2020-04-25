export class PostState {
  constructor(
    public type: string,
    public postId: string,
    public title: string,
    public progress: number,
    public status: string,
    public removed: boolean,
    public url: string,
    public done: number,
    public total: number,
    public hosts: string[],
    public previews: string[],
    public thanked: boolean,
    public alternativeTitle: string[]
  ) {}
}
