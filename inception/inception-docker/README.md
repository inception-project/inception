# INCEpTION Docker

You can use this module to build a Docker image either as part of the main build from the project
root or individually by only building this module. If you only build this module, make sure that
you have previously built the entire project so the Docker image can be created using the latest
INCEpTION (`inception-app-webapp`) artifact. 

## SNAPSHOT builds
To create a SNAPSHOT build:

    mvn -Pdocker clean docker:build
    mvn -Pdocker docker:push   (only if the build should be published)

To run the latest SNAPSHOT build, use: 

    docker run -p8080:8080 -v inception-data:/export -it ghcr.io/inception-project/inception-snapshots

To run the latest SNAPSHOT build with Docker Compose, use:

    INCEPTION_IMAGE=ghcr.io/inception-project/inception-snapshots INCEPTION_VERSION=latest docker-compose -f ../inception-doc/src/main/resources/META-INF/asciidoc/admin-guide/scripts/docker-compose.yml -p inception up

## Release builds
   
For a release build:

    mvn -Pdocker clean docker:build -Ddocker.image.name="ghcr.io/inception-project/inception"
    mvn -Pdocker docker:push -Ddocker.image.name="ghcr.io/inception-project/inception"
        
    docker run -p8080:8080 -v inception-data:/export -it ghcr.io/inception-project/inception

## Multi-platform build for ARM46 and AMD64

To build a docker images which runs on AMD64 as well as ARM64 machines, use:

    docker buildx create --use          (need to do this only once!)
    mvn -Pdocker-buildx clean package   (will be pushed immediately!)

Mind that you may have to log in to GitHub before being able to publish. This can be done e.g.
via Docker Desktop or on the command line via `docker login`.

## Options
If you want to keep the application data easily accessible in a folder on your host (e.g. if you
want to use a custom settings.properties file), you can provide a path on the host to the `-v` 
parameter.

    docker run -v /path/on/host/inception/repository:/export ...

## More on Docker Compose
In order to use **docker-compose**, specify 

export INCEPTION_HOME=/path/on/host/inception
export INCEPTION_PORT=port-on-host

In the folder where the **inception-doc/src/main/resources/META-INF/asciidoc/admin-guide/scripts/docker-compose.yml** is located, call

    docker-compose -p inception up -d
    
This starts an INCEpTION instance together with a MariaDB database.   
    
To stop, call

    docker-compose -p inception down
