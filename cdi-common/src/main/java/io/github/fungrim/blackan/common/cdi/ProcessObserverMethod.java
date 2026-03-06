package io.github.fungrim.blackan.common.cdi;

public final class ProcessObserverMethod {

    private final ObserverMethod observerMethod;
    private boolean vetoed;

    public ProcessObserverMethod(ObserverMethod observerMethod) {
        this.observerMethod = observerMethod;
    }

    public ObserverMethod observerMethod() {
        return observerMethod;
    }

    public void veto() {
        this.vetoed = true;
    }

    public boolean isVetoed() {
        return vetoed;
    }
}
