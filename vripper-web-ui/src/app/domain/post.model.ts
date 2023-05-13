export class Post {
  constructor(
    public postId: string,
    public postTitle: string,
    public status: string,
    public url: string,
    public done: number,
    public total: number,
    public hosts: string[],
    public addedOn: string,
    public rank: number,
    public downloadDirectory: string
  ) {}
}
