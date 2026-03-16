FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG MODULE_NAME
WORKDIR /workspace

COPY pom.xml ./
COPY flashmall-common/pom.xml flashmall-common/pom.xml
COPY flashmall-gateway/pom.xml flashmall-gateway/pom.xml
COPY flashmall-user/pom.xml flashmall-user/pom.xml
COPY flashmall-goods/pom.xml flashmall-goods/pom.xml
COPY flashmall-order/pom.xml flashmall-order/pom.xml
COPY flashmall-stock/pom.xml flashmall-stock/pom.xml

RUN mvn -pl ${MODULE_NAME} -am dependency:go-offline -DskipTests

COPY . .
RUN mvn -pl ${MODULE_NAME} -am clean package -DskipTests

FROM eclipse-temurin:21-jre

ARG MODULE_NAME
ENV TZ=Asia/Shanghai
WORKDIR /app

COPY --from=build /workspace/${MODULE_NAME}/target/${MODULE_NAME}-*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
