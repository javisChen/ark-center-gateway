# 介绍
网关

# 运行

## Docker Build
```shell
docker build -f ./ark-center-gateway-start/Dockerfile -t ark-center-gateway:v1 ./ark-center-gateway-start
```
## Docker Run
```shell
docker run --name ark-center-gateway -d -p 8082:8080 \
-e NACOS_DISCOVERY_IP=172.24.80.20 \
-e NACOS_DISCOVERY_SERVER_ADDR=172.24.80.20:8848 \
-e NACOS_CONFIG_SERVER_ADDR=172.24.80.20:8848 \
-e SYS_OPT=-DSpring.profiles.active=dev \
ark-center-gateway:v1
```