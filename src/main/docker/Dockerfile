FROM docker.io/eclipse-temurin:21-alpine

EXPOSE 8080

COPY build/libs/branch-deployer*.jar /opt/boudicca-branchdeployer/branchdeployer.jar
WORKDIR /opt/boudicca-branchdeployer/
CMD ["java", "-Dserver.port=8080", "-jar", "branchdeployer.jar"]