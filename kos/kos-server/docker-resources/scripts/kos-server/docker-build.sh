WORKSPACE="/docker-workspace/kubik/kos/kos-server"

unzip -o $WORKSPACE/target/docker.zip -d $WORKSPACE/target

docker build -t cspinformatique/kos $WORKSPACE/target/docker
