package io.github.fungrim.blackan.common.cdi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AfterDeploymentValidation {

    private final List<Throwable> problems = new ArrayList<>();

    public void addDeploymentProblem(Throwable t) {
        problems.add(t);
    }

    public List<Throwable> getProblems() {
        return Collections.unmodifiableList(problems);
    }
}
