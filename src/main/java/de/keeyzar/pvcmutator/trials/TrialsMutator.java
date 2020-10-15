package de.keeyzar.pvcmutator.trials;

import de.keeyzar.pvcmutator.pojo.trials.Trial;
import de.keeyzar.pvcmutator.utils.KFEConstants;
import de.keeyzar.pvcmutator.utils.SharedTrialNameList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TrialsMutator {
    private static final Logger log = LoggerFactory.getLogger(TrialsAdmissionController.class);
    private SharedTrialNameList sharedTrialNameList;

    @Inject
    public TrialsMutator(SharedTrialNameList sharedTrialNameList) {
        this.sharedTrialNameList = sharedTrialNameList;
    }

    boolean shouldWeModifyCorrespondingJobs(Trial trial) {
        //each trial not in kubeflow will have connection problems...
        boolean shouldModifyCorrespondingJobs = !"kubeflow".equals(trial.getMetadata().getNamespace());
        log.info("Should we modify corresponding jobs? {}", shouldModifyCorrespondingJobs);
        return shouldModifyCorrespondingJobs;
    }

    void mutateTrial(Trial trial) {
        //this is rather not necessary.. But to be sure, so everyone knows, we modified that.
        log.info("adding {} label", KFEConstants.KF_EXTENSION_LABEL);
        trial.getMetadata().getLabels().put(KFEConstants.KF_EXTENSION_LABEL, "true");



        String trialName = trial.getMetadata().getName();
        log.info("placing trial name: {} into list of necessary modifications", trialName);
        boolean added = sharedTrialNameList.getTrialNames().add(trialName);
        if (!added){
            log.error("trial with name {} was already found in list to modify", trialName);
            log.error("all trials to be modified:");
            sharedTrialNameList.getTrialNames().forEach(
                    e -> log.error("trialName: {}", e)
            );
        }
    }
}
