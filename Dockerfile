FROM maven:3.8.1-jdk-11-slim AS build

RUN mkdir /build
COPY . /build

WORKDIR /build
RUN mvn package -Dmaven.test.skip=true -T 2C


FROM openjdk:11.0.11-jre-slim AS run

ARG VERSION
ENV VERSION=${VERSION:-unspecified}
ENV JAR_FILE=vripper-server-${VERSION}-web.jar
ENV VRIPPER_DIR=/vripper

RUN mkdir ${VRIPPER_DIR}
COPY --from=build /build/vripper-server/target/${JAR_FILE} ${VRIPPER_DIR}
WORKDIR ${VRIPPER_DIR}
RUN mkdir downloads

EXPOSE 8080/tcp

CMD java -Dbase.dir.name=base -Duser.home=${VRIPPER_DIR}/downloads -jar ${VRIPPER_DIR}/${JAR_FILE}
