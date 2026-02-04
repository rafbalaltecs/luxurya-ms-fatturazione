package it.fatturazione.service.auth.impl;

import it.fatturazione.exception.AuthException;
import it.fatturazione.service.auth.AuthService;
import it.fatturazione.service.cache.RedisService;
import it.fatturazione.shared.UserDataShared;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private UserDataShared userDataShared;
    private final RedisService redisService;

    public AuthServiceImpl(RedisService redisService) {
        this.redisService = redisService;
    }

    public UserDataShared getUserDataShared() {
        return userDataShared;
    }


    @Override
    public Boolean existValidToken(String token) {
        if(StringUtils.isNotEmpty(token)){
           return redisService.exists(token);
        }
        return Boolean.FALSE;
    }

    @Override
    public void userFindByToken(String token) {
        if(StringUtils.isNotEmpty(token)){
            userDataShared = (UserDataShared) redisService.get(token);
        }else{
            log.error("Token Not Valid: {}", token);
            throw new AuthException("Token Not Valid, execute Login Action to ms-anagrafica");
        }
    }
}
