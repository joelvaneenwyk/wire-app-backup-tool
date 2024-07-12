FROM adoptopenjdk/openjdk11:alpine AS build
LABEL description="Wire Export Backup Tool"
LABEL project="wire-bots:exports"

ENV PROJECT_ROOT=/source
WORKDIR $PROJECT_ROOT

# Copy gradle settings
COPY \
    settings.gradle.kts gradle.properties gradlew \
    $PROJECT_ROOT/

# Make sure gradlew is executable
RUN chmod +x gradlew
# Copy gradle specification
COPY ./gradle/ $PROJECT_ROOT/gradle/
COPY ./libs/ $PROJECT_ROOT/libs/
COPY ./build-logic/ $PROJECT_ROOT/build-logic/
# Download gradle
RUN ./gradlew --version --no-daemon
# download and cache dependencies
RUN ./gradlew resolveDependencies --no-daemon

# Copy project and build
COPY ./app/ $PROJECT_ROOT/app/
RUN ./gradlew shadowJar --no-daemon

# Runtime
FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add bash

ENV APP_ROOT=/app
ENV PROJECT_APP_SOURCE_DIR=$PROJECT_ROOT/app
WORKDIR $APP_ROOT

# Obtain built from the base
COPY --from=build $PROJECT_APP_SOURCE_DIR/build/libs/backup-export.jar $APP_ROOT/

# copy entrypoint
COPY entrypoint.sh $APP_ROOT/entrypoint.sh
# ensure it is executable
RUN chmod +x $APP_ROOT/entrypoint.sh

# copy database decryption lib
RUN mkdir -p $APP_ROOT/libs/
COPY ./libs/libsodium.so $APP_ROOT/libs/

# execute run script
ENTRYPOINT ["/bin/sh", "-c", "$APP_ROOT/entrypoint.sh"]
