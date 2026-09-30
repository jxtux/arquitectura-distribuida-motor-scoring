package com.finanscore.motorscoring.application.security.port.in;

import com.finanscore.motorscoring.application.security.model.UserAccount;

public interface GetCurrentUserUseCase {
    UserAccount get(Long userId);
}
