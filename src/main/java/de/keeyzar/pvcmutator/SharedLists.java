package de.keeyzar.pvcmutator;

import io.vertx.core.impl.ConcurrentHashSet;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class SharedLists {
    private Set<String> trialList = new ConcurrentHashSet<>();
    private Set<String> jobList = new ConcurrentHashSet<>();

    public Set<String> getTrialList() {
        return trialList;
    }
    public void setTrialList(Set<String> trialList) {
        this.trialList = trialList;
    }

    public Set<String> getJobList() {
        return jobList;
    }

    public void setJobList(Set<String> jobList) {
        this.jobList = jobList;
    }
}
