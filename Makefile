build:
	./gradlew shadowJar

docker-build:
	docker build -t joelvaneenwyk/wire-app-backup-tool .

docker-push:
	docker push joelvaneenwyk/wire-app-backup-tool
