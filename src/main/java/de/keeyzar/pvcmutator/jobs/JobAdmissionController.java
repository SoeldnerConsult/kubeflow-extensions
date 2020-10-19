package de.keeyzar.pvcmutator.jobs;

import de.keeyzar.pvcmutator.utils.AdmissionReviewMutatorHelper;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.batch.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/job/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class JobAdmissionController {
    private static final Logger log = LoggerFactory.getLogger(JobAdmissionController.class);
    private final AdmissionReviewMutatorHelper admissionReviewMutatorHelper;
    private final JobMutator jobMutator;

    @Inject
    public JobAdmissionController(AdmissionReviewMutatorHelper admissionReviewMutatorHelper, JobMutator jobMutator){
        this.admissionReviewMutatorHelper = admissionReviewMutatorHelper;
        this.jobMutator = jobMutator;
    }

    @POST
    public AdmissionReview mutate(AdmissionReview review) {
        log.info("received Job AdmissionReview");
        return admissionReviewMutatorHelper.createAdmissionReview(review, Job.class,
                jobMutator::doesJobNeedModification, jobMutator::mutateJob);
    }

}