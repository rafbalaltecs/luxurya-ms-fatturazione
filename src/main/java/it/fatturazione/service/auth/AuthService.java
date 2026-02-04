package it.fatturazione.service.auth;

import it.fatturazione.shared.UserDataShared;

public interface AuthService {
    Boolean existValidToken(final String token);
    void userFindByToken(final String token);
    UserDataShared getUserDataShared();
}
