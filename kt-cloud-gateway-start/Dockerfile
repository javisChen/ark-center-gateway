FROM openjdk:11-oraclelinux8
WORKDIR /app
COPY ./target/kt-cloud-gateway.jar /app
EXPOSE 8082
ENV SYS_OPT=""
ENTRYPOINT java $SYS_OPT -jar /app/kt-cloud-gateway.jar