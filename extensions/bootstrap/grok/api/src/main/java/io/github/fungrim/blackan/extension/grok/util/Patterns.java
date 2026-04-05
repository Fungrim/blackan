package io.github.fungrim.blackan.extension.grok.util;

import java.util.List;

import io.whatap.grok.api.Grok;

public class Patterns {
    
    public static boolean anyMatches(Grok grok, List<String> strings) {
        return strings.stream().anyMatch(string -> stringMatches(grok, string));
    }

    public static boolean stringMatches(Grok grok, String string) {
        var matcher = grok.match(string).getMatch();
        return matcher != null && matcher.matches();
    }

    public static boolean anyMatches(String value, List<Grok> patterns) {
        return patterns.stream().anyMatch(grok -> stringMatches(grok, value));
    }
}
