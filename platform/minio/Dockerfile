FROM golang:1.24-alpine AS builder

RUN apk add --no-cache git ca-certificates

ENV CGO_ENABLED=0

RUN go install github.com/minio/minio@RELEASE.2025-09-07T16-13-09Z


FROM alpine:3.22

RUN apk add --no-cache ca-certificates tzdata

COPY --from=builder /go/bin/minio /usr/local/bin/minio

EXPOSE 9000 9001

VOLUME ["/data"]

ENTRYPOINT ["minio"]