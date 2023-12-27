FROM openjdk:21-oraclelinux8

#RUN microdnf install -y iputils

WORKDIR /app

COPY ark-center-gateway.jar /app

EXPOSE 8082

ENV SYS_OPT=""

ENTRYPOINT java $SYS_OPT -jar /app/ark-center-gateway.jar