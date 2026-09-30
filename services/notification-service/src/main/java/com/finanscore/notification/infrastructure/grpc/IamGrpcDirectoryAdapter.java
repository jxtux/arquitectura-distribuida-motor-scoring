package com.finanscore.notification.infrastructure.grpc;

import com.finanscore.contracts.iam.v1.*;
import com.finanscore.notification.application.IamUserDirectoryPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

/** Implementación real V3.6: gRPC + mTLS + OAuth2 Client Credentials. */
@Component
public class IamGrpcDirectoryAdapter implements IamUserDirectoryPort {
    private final ManagedChannel channel;
    private final UserDirectoryGrpc.UserDirectoryBlockingStub stub;
    private final int deadlineMs;

    public IamGrpcDirectoryAdapter(
            InternalOAuth2TokenClient tokens,
            @Value("${app.iam.grpc-host:iam-service}") String host,
            @Value("${app.iam.grpc-port:9091}") int port,
            @Value("${app.iam.mtls.ca-cert:/run/secrets/ca.crt}") String caCert,
            @Value("${app.iam.mtls.client-cert:/run/secrets/notification-grpc-client.crt}") String clientCert,
            @Value("${app.iam.mtls.client-key:/run/secrets/notification-grpc-client.key}") String clientKey,
            @Value("${app.iam.read-timeout-ms:3000}") int deadlineMs) {
        try {
            var ssl = GrpcSslContexts.forClient()
                    .trustManager(new File(caCert))
                    .keyManager(new File(clientCert), new File(clientKey))
                    .build();
            this.channel = NettyChannelBuilder.forAddress(host, port).sslContext(ssl).build();
            Channel authorized = ClientInterceptors.intercept(channel, new OAuth2BearerClientInterceptor(tokens));
            this.stub = UserDirectoryGrpc.newBlockingStub(authorized);
            this.deadlineMs = deadlineMs;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo configurar gRPC mTLS hacia IAM", ex);
        }
    }

    @Retry(name = "iamDirectory")
    @CircuitBreaker(name = "iamDirectory")
    @Bulkhead(name = "iamDirectory", type = Bulkhead.Type.SEMAPHORE)
    @Override
    public UserInfo get(Long id) {
        UserReply reply = stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                .getUser(GetUserRequest.newBuilder().setUserId(id).build());
        return new UserInfo(reply.getUserId(), reply.getDisplayName(), reply.getEmail(), reply.getStatus());
    }

    @PreDestroy
    public void close() {
        channel.shutdown();
        try { channel.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        channel.shutdownNow();
    }
}
