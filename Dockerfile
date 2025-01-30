FROM openjdk:21

COPY /build/libs/google-calendar-1.0.0-SNAPSHOT.jar google-calendar.jar
COPY /locale/ /locale/