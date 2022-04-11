# 介绍
网关

# 运行

## Docker Build
```shell
docker build -f ./kt-cloud-gateway-start/Dockerfile -t kt-cloud-gateway:v1 ./kt-cloud-gateway-start
```
## Docker Run
```shell
docker run --name kt-cloud-gateway -d -p 8082:8080 \
-e SPRING.CLOUD.NACOS.DISCOVERY.IP=172.24.80.20 \
-e DISCOVERY-SERVER-ADDR=172.24.80.20:8848 \
-e CONFIG-SERVER-ADDR=172.24.80.20:8848 \
-e SYS_OPT=-DSpring.profiles.active=dev \
kt-cloud-gateway:v1
```