For a SNAPSHOT build:

  mvn -Pdocker clean install docker:build
  mvn -Pdocker clean install docker:push   (only if the build should be published)
  
For a release build:

  mvn -Pdocker clean install docker:build -Ddocker.image.name="inceptionproject/inception"
  mvn -Pdocker clean install docker:push -Ddocker.image.name="inceptionproject/inception"

The container than can be started by executing

    docker run -v /path/on/host/inception/repository:/export -p port-on-host:8080 inceptionproject/inception:latest
    
In order to use **docker-compose**, specify 

export INCEPTION_HOME=/path/on/host/inception
export INCEPTION_PORT=port-on-host

In the folder where the **docker-compose.yml** is located, call

    docker-compose up -d
    
This starts an INCEpTION instance together with a MySQL database.   
    
To stop, call

    docker-compose down