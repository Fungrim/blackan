package io.github.fungrim.blackan.common.cdi;

import org.jboss.jandex.ClassInfo;

public final class ProcessAnnotatedType {

    private final ClassInfo type;
    private boolean vetoed;

    public ProcessAnnotatedType(ClassInfo type) {
        this.type = type;
    }

    public ClassInfo type() {
        return type;
    }

    public void veto() {
        this.vetoed = true;
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
