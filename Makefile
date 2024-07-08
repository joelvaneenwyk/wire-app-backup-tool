build:
	./gradlew shadowJar

docker-build:
	docker build -t joelvaneenwyk/backup-export-tool .

docker-push:
	docker push joelvaneenwyk/backup-export-tool
