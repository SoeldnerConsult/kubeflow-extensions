package de.keeyzar.pvcmutator.jobs;

import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.batch.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JobMutator {
    private static final Logger log = LoggerFactory.getLogger(JobAdmissionController.class);

    boolean doesJobNeedModification(Job job) {
        String expectedValue = "true";
        String defaultValue = "false";

        String kubeflowExtensionValue = job.getMetadata().
                getLabels().getOrDefault(KFEConstants.KF_EXTENSION_LABEL, defaultValue);

        log.info("Does the Job contains the Label: {}? {}", KFEConstants.KF_EXTENSION_LABEL,
                job.getMetadata().getLabels().containsKey(KFEConstants.KF_EXTENSION_LABEL));

        return expectedValue.equals(kubeflowExtensionValue);
    }

    void mutateJob(Job job) {
        log.info("mutating job, adding {} label", KFEConstants.KF_EXTENSION_LABEL);
        job.getSpec().getTemplate().getMetadata().getLabels().put(KFEConstants.KF_EXTENSION_LABEL, "true");
    }
}
