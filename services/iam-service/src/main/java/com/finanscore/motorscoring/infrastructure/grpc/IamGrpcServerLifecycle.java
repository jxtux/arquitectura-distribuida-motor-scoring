package com.finanscore.motorscoring.infrastructure.grpc;

import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.*;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Component
public class IamGrpcServerLifecycle implements SmartLifecycle {
	private static final Logger log = LoggerFactory.getLogger(IamGrpcServerLifecycle.class);

	private final int port;
	private final String cert;
	private final String key;
	private final String ca;
	private final UserDirectoryGrpcService service;
	private final GrpcBearerTokenServerInterceptor authInterceptor;
	private volatile Server server;
	private volatile boolean running;

	public IamGrpcServerLifecycle(@Value("${iam.grpc.port:9091}") int port,
			@Value("${iam.grpc.server-cert:/run/secrets/iam-grpc-server.crt}") String cert,
			@Value("${iam.grpc.server-key:/run/secrets/iam-grpc-server.key}") String key,
			@Value("${iam.grpc.ca-cert:/run/secrets/ca.crt}") String ca, UserDirectoryGrpcService service,
			GrpcBearerTokenServerInterceptor authInterceptor) {
		this.port = port;
		this.cert = cert;
		this.key = key;
		this.ca = ca;
		this.service = service;
		this.authInterceptor = authInterceptor;
	}

	//IAM exige obligatoriamente certificado cliente. 
	//“Una conexión sin certificado válido ni siquiera llega al método gRPC.”
	
	@Override
	public synchronized void start() {
		if (running)
			return;
		try {
			var sslContext = GrpcSslContexts.forServer(new File(cert), new File(key)).trustManager(new File(ca)).clientAuth(ClientAuth.REQUIRE).build();
			server = NettyServerBuilder.forPort(port).sslContext(sslContext).addService(ServerInterceptors.intercept(service, authInterceptor)).build().start();
			running = true;
			log.info("IAM gRPC mTLS listening on port {}", port);
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo iniciar IAM gRPC mTLS", ex);
		}
	}

	@Override
	public synchronized void stop() {
		if (server != null) {
			server.shutdown();
			try {
				server.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			server.shutdownNow();
		}
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE - 100;
	}
}
