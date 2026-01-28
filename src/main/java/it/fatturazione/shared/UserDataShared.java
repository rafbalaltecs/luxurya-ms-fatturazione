package it.fatturazione.shared;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserDataShared {
    private String token;
    private String username;
    private Set<String> routes;
    private Long userId;
    private LocalDateTime loginAt;
}
