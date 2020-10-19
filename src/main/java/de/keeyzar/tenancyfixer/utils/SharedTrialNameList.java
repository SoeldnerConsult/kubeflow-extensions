package de.keeyzar.tenancyfixer.utils;

import io.vertx.core.impl.ConcurrentHashSet;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

/**
 * this class is necessary so we can identify corresponding jobs
 * we save the name of the trial, and identify created jobs with identical names
 */
@ApplicationScoped
public class SharedTrialNameList {
    private Set<String> trialNames = new ConcurrentHashSet<>();

    public Set<String> getTrialNames() {
        return trialNames;
    }

    public void setTrialNames(Set<String> trialNames) {
        this.trialNames = trialNames;
    }
}
