package io.github.fungrim.blackan.injector.lookup.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(200)
public class HighPriorityAlternativeBean implements TestService {
}
