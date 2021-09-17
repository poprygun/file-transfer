# Spring Integration Flow

Download files from URL and upload to S3.
Programe iterates over json elements of `resources/config/load-config.json` extracting pin to construct the url of the file to be downloaded.

## Local Development

Start the application locally

```bash
./gradlew bootRun
```

## Minio setup

```bash
docker run \
  -p 9000:9000 \
  -p 9001:9001 \
  minio/minio server /data --console-address ":9001"
```

Login to [minio console](http://127.0.0.1:9000) using `minioadmin:minioadmin` credentials, and create a bucket - default is `chachkies`.

`application.yml` Sets properties required to connect to Minio instance.  Application is using following defaults.

```yaml
s3:
  user: minioadmin
  password: minioadmin
  url: 'http://localhost:9000'
```