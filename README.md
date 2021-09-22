# Spring Integration Flow

Download files from URL and upload to S3.
Program iterates over json elements of `resources/config/load-config.json` extracting pin to construct the url of the file to be downloaded.

Trigger execution of the flow using:

```bash
curl --location --request GET 'http://localhost:8080/flow'
```

Counter for uploaded documents is expose via [prometheus actuator endpoint](http://localhost:8080/actuator/prometheus).
Search for following lines on the actuator status page:

```
# HELP uploaded_tm_total The number of PDF files uploaded to S3.
# TYPE uploaded_tm_total counter
uploaded_tm_total{type="pdf",} 0.0
```

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