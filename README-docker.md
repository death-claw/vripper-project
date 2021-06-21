# VRipper! In Docker!

_WARNING_: This is experimental. Not familiar with Docker? This isn't for you, yet.

Java is nice. Java is cross-platform. But, it can be a challenge for people unfamiliar with Java to run and use an app built it. Docker can help.

## How to build

You need a recent version of Docker (tested with `20.10.2`).

The [`Dockerfile`](Dockerfile) will build the VRipper _Server_ app and then build it into a Docker image.

To build the server app as a Docker image:

    docker build --build-arg VERSION=3.5.4 --tag=death-claw/vripper-project:3.5.4 --file=Dockerfile .

## How to run

You could use `docker run` but `docker-compose` makes it more convenient. You need a recent version of `docker-compose` (tested with `1.25.0`).

Change ports and volumes in [`docker-compose.yml`](docker-compose.yml) if necessary. Then, to run the server app container in the background:

    docker-compose up --detach

## How to use

Use the Server app as you would, normally. See [`README.md`](README.md).

## Future

If **death-claw** wishes:

- Run VRipper in container as non-root user.
- Resolve non-root user file permissions.
- Automatically build and tag a Docker image for each tagged version of VRipper.
- Push images to the GitHub Packages Docker registry.
- Write end-user instructions for running images pulled from GitHub registry.
