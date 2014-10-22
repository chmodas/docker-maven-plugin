# docker-maven-plugin

This is a Maven plugin for managing [Docker](https://www.docker.com/) images and containers from within Maven builds.

## Maven goals

### Image

| Parameter      | Description                                                     | Default        |
|----------------|-----------------------------------------------------------------|----------------|
| __name__       | The name of the container, essential for container linking      | none, required |
| __repository__ | Image repository (e.g. example.com/postgres, username/postgres) | none, required |
| __tag__        | The image repository tag                                        | latest         |
| __command__    | The command to run inside the container                         | none           |
| __ports__      | List of [hostPort:exposedPort](#ports) entries.                 | none           |
| __volumes__    | List of volumes. (e.g. /volume, /host:/volume)                  | none           |

### `docker:start`

Creates and starts docker containers. 

#### Configuration

| Parameter   | Description                                                 | Property           | Default                   |
|-------------|-------------------------------------------------------------|--------------------|---------------------------|
| __url__     | URL to the Docker daemon API                                | __docker.url__     | __http://localhost:4243__ |
| __version__ | Docker API version to use                                   | __docker.version__ | __1.14__                  |
| __prefix__  | A prefix used in the container names                        | __docker.prefix__  | __project.artifactId__    |
| __images__  | List of [images](#image) parameters for starting containers | __docker.images__  | none                      |

### `docker:stop`

Stop docker containers.

#### Configuration

| Parameter   | Description                                                 | Property           | Default                   |
|-------------|-------------------------------------------------------------|--------------------|---------------------------|
| __url__     | URL to the Docker daemon API                                | __docker.url__     | __http://localhost:4243__ |
| __version__ | Docker API version to use                                   | __docker.version__ | __1.14__                  |
| __prefix__  | A prefix used in the container names                        | __docker.prefix__  | __project.artifactId__    |
| __images__  | List of [images](#image) parameters for starting containers | __docker.images__  | none                      |
