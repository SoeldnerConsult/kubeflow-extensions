package de.keeyzar.pvcmutator.trials;

import de.keeyzar.pvcmutator.pojo.Trials.Trial;
import de.keeyzar.pvcmutator.utils.KFEConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TrialsMutator {
    private static final Logger log = LoggerFactory.getLogger(TrialsAdmissionController.class);

    boolean shouldWeModifyCorrespondingJobs(Trial trial) {
        //each trial not in kubeflow will have connection problems...
        boolean shouldModifyCorrespondingJobs = !"kubeflow".equals(trial.getMetadata().getNamespace());
        log.info("Should we modify corresponding jobs? {}", shouldModifyCorrespondingJobs);
        return shouldModifyCorrespondingJobs;
    }

    void mutateTrial(Trial trial) {
        log.info("adding {} label", KFEConstants.KF_EXTENSION_LABEL);
        trial.getMetadata().getLabels().put(KFEConstants.KF_EXTENSION_LABEL, "true");
    }
}
