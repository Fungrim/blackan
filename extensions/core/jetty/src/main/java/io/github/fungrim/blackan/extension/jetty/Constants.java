package io.github.fungrim.blackan.extension.jetty;

import java.nio.file.Path;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Constants {

    public static final Path APPLICATION_SCOPE_KEY = Path.of("blackan/application-scope");
    public static final Path SESSION_SCOPE_KEY = APPLICATION_SCOPE_KEY.resolve("session-scope");
    public static final Path REQUEST_SCOPE_KEY = SESSION_SCOPE_KEY.resolve("request-scope");
    
    
}
