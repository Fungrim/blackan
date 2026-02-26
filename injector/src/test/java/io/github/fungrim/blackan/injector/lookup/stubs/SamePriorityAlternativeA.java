package io.github.fungrim.blackan.injector.lookup.stubs;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(100)
public class SamePriorityAlternativeA implements TestService {
}
