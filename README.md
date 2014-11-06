# docker-maven-plugin

This docker-maven-plugin is a Maven plugin designed to make managing [Docker](https://www.docker.com/) images and containers from within Maven builds easy.

## Goals Overview
* [docker:start](#) - Create and start docker containers
* [docker:stop](#) - Stop and remove docker containers

## Usage

In order to use the docker-maven-plugin, you need to add the following configuration to your pom.xml file.

```xml
    <project>
        [...]
        <build>
            [...]
            <plugins>
                [...]
                <plugin>
                    <groupId>com.github.chmodas</groupId>
                    <artifactId>docker-maven-plugin</artifactId>
                    <version>0.0.3</version>

                    <!-- common configuration shared by all executions -->
                    <configuration>
                        <url></url>
                        <images>
                            <image>
                                <name></name>
                                <repository></repository>
                                <tag></tag>
                                <ports>
                                    <port></port>
                                </ports>
                            </image>
                        </images>
                    </configuration>

                    <executions>
                        <execution>
                            <phase></phase>
                            <goals>
                                <goal>start</goal>
                            </goals>
                        </execution>
                        <execution>
                            <phase></phase>
                            <goals>
                                <goal>stop</goal>
                            </goals>
                        </execution>
                    </executions>

                </plugin>
                [...]
            </plugins>
            [...]
        </build>
        [...]
    </project>
```

### General Configuration

| Parameter   | Description                                                 | Property           | Default                   |
|-------------|-------------------------------------------------------------|--------------------|---------------------------|
| __url__     | URL to the Docker daemon API                                | __docker.url__     | __http://localhost:4243__ |
| __version__ | Docker API version to use                                   | __docker.version__ | __1.14__                  |
| __prefix__  | Prefix used when naming containers                          | __docker.prefix__  | __project.artifactId__    |
| __images__  | List of [image](#image) parameters for starting containers  | __docker.images__  | none                      |

### Image Configuration

| Parameter      | Description                                                                  | Default        |
|----------------|------------------------------------------------------------------------------|----------------|
| __name__       | The name of the container, essential for container linking                   | none, required |
| __repository__ | Image repository (e.g. example.com/postgres, username/postgres)              | none, required |
| __tag__        | The image repository tag                                                     | latest         |
| __command__    | Command to execute inside the container on start                             | none           |
| __ports__      | Collection of ports to publish                                               | none           |
| __ports/port__ | Docker exposed port to publish.  Format is [hostPort:exposedPort]            | none           |
| __volumes__    | List of volumes to mount inside the container. (e.g. /volume, /host:/volume) | none           |
| __links__      | List of links to containers                                                  | none           |
| __wait__       | Sleep for given amount of seconds after container has been started           | 0              |

## Best Practices

While you have the ability to specify images to start / stop in the configuration section of the start and stop goals,
this is probably not what you want to do.  Defining all of your images in the general configuration section will ensure that all of
the containers are started and stopped properly.
