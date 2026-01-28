package it.fatturazione.service.auth;

import it.fatturazione.shared.UserDataShared;

public interface AuthService {
    void userFindByToken(final String token);
    UserDataShared getUserDataShared();
}
