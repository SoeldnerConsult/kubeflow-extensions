package de.keeyzar.tenancyfixer.jobs;

import de.keeyzar.tenancyfixer.utils.KFEConstants;
import de.keeyzar.tenancyfixer.utils.SharedTrialNameList;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class JobMutator {
    private static final Logger log = LoggerFactory.getLogger(JobAdmissionController.class);
    private final SharedTrialNameList sharedTrialNameList;

    @Inject
    public JobMutator(SharedTrialNameList sharedTrialNameList) {
        this.sharedTrialNameList = sharedTrialNameList;
    }

    boolean doesJobNeedModification(Job job) {
        log.info("searching job with name {} in our to be modified list", job.getMetadata().getName());
        boolean removedObject = sharedTrialNameList.getTrialNames().remove(job.getMetadata().getName());
        if(!removedObject){
            log.info("jobname not found in jobs which must be modified");
            log.info("all possible jobs:");
            sharedTrialNameList.getTrialNames().forEach(
                    e -> log.info("Jobs to modify: {}", e)
            );
        }

        return removedObject;
    }

    void mutateJob(Job job) {
        log.info("mutating job, adding {} label", KFEConstants.KF_EXTENSION_LABEL);
        PodTemplateSpec template = job.getSpec().getTemplate();

        //istio label must be fixed at this point. Because otherwise we may come into
        //racing conditions, because the istio mutating webhook is called earlier as
        //our pod mutating webhook...
        template.getMetadata().getAnnotations().put("sidecar.istio.io~1inject", "true");

        //this may or may not be added here.. It's just a label.
        //we're already modifying stuff here, therefore we may add it here..
        template.getSpec().setServiceAccountName("default-editor");
        template.getMetadata().getLabels().put(KFEConstants.KF_EXTENSION_LABEL, "true");
    }
}
