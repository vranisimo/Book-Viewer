FROM openjdk:11-jre-slim
COPY gcs_credentials.json .
COPY .docker/installMagick.sh .
EXPOSE 8080
ADD /build/libs/book_viewer-0.0.1-SNAPSHOT.jar book_viewer.jar
#ENTRYPOINT ["java", "-jar", "book_viewer.jar"]
ENTRYPOINT []
