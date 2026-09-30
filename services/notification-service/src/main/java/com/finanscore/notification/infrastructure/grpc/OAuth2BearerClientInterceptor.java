package com.finanscore.notification.infrastructure.grpc;

import io.grpc.*;

public class OAuth2BearerClientInterceptor implements ClientInterceptor {
    private static final Metadata.Key<String> AUTHORIZATION =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
    private final InternalOAuth2TokenClient tokens;

    public OAuth2BearerClientInterceptor(InternalOAuth2TokenClient tokens) {
        this.tokens = tokens;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        ClientCall<ReqT, RespT> delegate = next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<>(delegate) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(AUTHORIZATION, "Bearer " + tokens.accessToken());
                super.start(responseListener, headers);
            }
        };
    }
}
