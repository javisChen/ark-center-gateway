FROM openjdk:11-oraclelinux8
WORKDIR /app
COPY ./target/ark-center-gateway.jar /app
EXPOSE 8082
ENV SYS_OPT=""
ENTRYPOINT java $SYS_OPT -jar /app/ark-center-gateway.jar