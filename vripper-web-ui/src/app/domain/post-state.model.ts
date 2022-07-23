export class Post {
  constructor(
    public postId: string,
    public postTitle: string,
    public progress: number,
    public status: string,
    public url: string,
    public done: number,
    public total: number,
    public hosts: string[],
    public metadata: Metadata,
    public addedOn: string,
    public rank: number
  ) {
  }
}

export interface Metadata {
  resolvedNames: string[];
  postedBy: string;
}
