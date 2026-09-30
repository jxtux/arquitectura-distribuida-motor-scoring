package com.finanscore.motorscoring.infrastructure.grpc;

import com.finanscore.contracts.iam.v1.*;
import com.finanscore.motorscoring.application.security.port.in.GetCurrentUserUseCase;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class UserDirectoryGrpcService extends UserDirectoryGrpc.UserDirectoryImplBase {
    private final GetCurrentUserUseCase users;

    public UserDirectoryGrpcService(GetCurrentUserUseCase users) {
        this.users = users;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserReply> observer) {
        try {
            var user = users.get(request.getUserId());
            observer.onNext(UserReply.newBuilder()
                    .setUserId(user.id())
                    .setDisplayName(user.displayName() == null ? "" : user.displayName())
                    .setEmail(user.email() == null ? "" : user.email())
                    .setStatus(user.status().name())
                    .build());
            observer.onCompleted();
        } catch (RuntimeException ex) {
            observer.onError(Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        }
    }
}
