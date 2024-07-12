# Backup export tool

[![GitHub version](https://badgen.net/github/release/joelvaneenwyk/wire-app-backup-tool)](https://github.com/joelvaneenwyk/wire-app-backup-tool/releases)
![CI/CD](https://github.com/joelvaneenwyk/wire-app-backup-tool/workflows/CI/CD/badge.svg)
![Release Pipeline](https://github.com/joelvaneenwyk/wire-app-backup-tool/actions/workflows/release.yml/badge.svg)

Tool for generating PDF files from the backup. All backup formats are supported (Desktop, iOS, Android).

Please note that this tool is not for public use, but was rather developed for specific enterprise customers. However, we believe in
open-source world, so if you manage to get it working, feel free to use it.

## Download

The latest stable version of the
tool: [![GitHub version](https://badgen.net/github/release/joelvaneenwyk/wire-app-backup-tool)](https://github.com/joelvaneenwyk/wire-app-backup-tool/releases). Please use this version as the tag for the docker image - `quay.io/joelvaneenwyk/wire-app-backup-tool:<version>`. If you'd like to run the tool on bare metal, download all assets on the [release page](https://github.com/joelvaneenwyk/wire-app-backup-tool/releases).

See available Docker image tags [here on Quay](https://quay.io/repository/joelvaneenwyk/wire-app-backup-tool?tab=tags).

## TL;DR

How to generate PDF files using Desktop (or Web) app backup file

```bash
java -jar backup-export.jar \
  web \
  "my_backup.desktop_wbu" \
  --email "dejan56@wire.com" \
  --password "MyCoolPasswordForWire1"
```

## Execution

There are two ways how to run the tool, using Docker (preferred) or Java.

### Docker

One should use attached [start.sh](./start.sh) to run the tool.
Fill variables first.

To run it manually inside the Docker execute following:

```bash
docker run --rm -it \
  -v "</path/to/database/file":/app/database-in \
  -v "</path/to/output/folder>":/app/database-out \
  -e CLIENT_TYPE="<web,ios,android>" \
  -e WIRE_USER="<user-email-with-wire-account>" \
  -e WIRE_PASSWORD="<password-for-that-user>" \
  -e BACKUP_PASSWORD="<optional-password-for-backup-file>" \
  -e BACKUP_USERNAME="<optional-username-who-created-backup>" \
  -e WIRE_API_HOST="<url-to-wire-backend>" \
  -e USE_PROXY="<true,false>" \
  -e PROXY_URL="<optional-proxy-url>" \
  -e PROXY_PORT="<optional-proxy-port>" \
  -e NON_PROXY_HOSTS="<optional-proxy-hosts>" \
  quay.io/joelvaneenwyk/wire-app-backup-tool:latest
```

Where `<some value>` should be replaced by your own value.
Following variables are optional:

- `PROXY_URL`, `PROXY_PORT`, `NON_PROXY_HOSTS` - proxy settings, use only if you need it, also
if you specify these values, you must also add `-e USE_PROXY=true` to activate proxy
- `WIRE_API_HOST` - URL to Wire backend, default value is used Wire public cloud
- `BACKUP_PASSWORD` and `BACKUP_USERNAME` - use only if `CLIENT_TYPE` is `ios` or `android`

Example:

```bash
docker run --rm -it \
  -v "backups/dejan56.ios_wbu":/app/database-in \
  -v "backup-exports/dejan":/app/database-out \
  -e CLIENT_TYPE="ios" \
  -e WIRE_USER="my-testing-user@wire.com" \
  -e WIRE_PASSWORD="VerySecretPassword1!" \
  -e BACKUP_PASSWORD="Monkey123!" \
  -e BACKUP_USERNAME="dejan56" \
  quay.io/joelvaneenwyk/wire-app-backup-tool:latest
```

### Bare Metal JVM

One needs C library Libsodium installed. To install it, one should use [official documentation](https://libsodium.gitbook.io/doc/),
or to use included binaries.
One should use Java 8 (OpenJDK) to run the tool, but Java 11 (OpenJDK) seems to be working as well.
We observed some problems running the tool under Windows and Oracle JDK, thus OpenJDK is required.

Almost all parameters are set using arguments to the tool, however, in order to use different Wire backend,
one must set environmental variable with backend URL:

```bash
WIRE_API_HOST="<your-wire-backend>"
```

For example to set it on unix systems: `export WIRE_API_HOST=https://staging-nginz-https.zinfra.io` for staging.

To create executable `jar` please run `./gradlew shadowJar` which produces `build/libs/backup-export.jar`.
Generic way how to run the tool is following:

```bash
java -Djna.library.path="<path-to-binaries>" \
  -Xmx4g \
  -jar "<path-to-jar>" \
  "<ios,android,web>" \
  "</path/to/database/file>" \
  --output "</path/to/output/folder>" \
  --email "<user-with-wire-account>" \
  --password "<password-for-that-user>" \
  --username"<username-who-created-backup>" \
  --backup-password "<password-for-backup-file>"
```

An example for iOS backup without proxy (the build is downloaded and extracted `zip` from the release page)

```bash
java -Djna.library.path="libs" \
  -Xmx4g \
  -jar "backup-export.jar" \
  ios \
  "backups/dejan56.ios_wbu" \
  --output "dejans-export" \
  --email "dejan56@wire.com" \
  --password "MyCoolPasswordForWire1" \
  --username "dejan56" \
  --backup-password "AnotherCoolPasswordForBackups"
```

If one needs to use proxy, `--use-proxy` must be set, see example:

```bash
java -Djna.library.path="libs" \
  -Xmx4g \
  -jar "backup-export.jar" \
  web \
  "backups/Wire-fredjones_Demo_Backup.zip" \
  --output "backup-exports" \
  --email "dejan56@wire.com" \
  --password "MyCoolPasswordForWire1" \
  --use-proxy "true" \
  --proxy-host "my-proxy-domain.com" \
  --proxy-port 8080 \
  --proxy-non-proxy-hosts "non-proxied-host.com"
```
