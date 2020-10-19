package de.keeyzar.pvcmutator.trials;

import de.keeyzar.pvcmutator.pojo.trials.Trial;
import de.keeyzar.pvcmutator.utils.AdmissionReviewMutatorHelper;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/trial/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TrialsAdmissionController {
    private static final Logger log = LoggerFactory.getLogger(TrialsAdmissionController.class);
    private final AdmissionReviewMutatorHelper admissionReviewMutatorHelper;
    private final TrialsMutator trialsMutator;

    @Inject
    public TrialsAdmissionController(AdmissionReviewMutatorHelper admissionReviewMutatorHelper,
                                     TrialsMutator trialsMutator){
        this.admissionReviewMutatorHelper = admissionReviewMutatorHelper;
        this.trialsMutator = trialsMutator;
    }

    public void preRegister(@Observes StartupEvent startupEvent){
        //todo is this necessary?
        //I guess yes, because of Lombok
        KubernetesDeserializer.registerCustomKind("kubeflow.org/v1alpha3", "Trial", Trial.class);
    }

    @POST
    public AdmissionReview mutate(AdmissionReview review) {
        log.info("received Trial AdmissionReview");
        return admissionReviewMutatorHelper.createAdmissionReview(review, Trial.class,
                trialsMutator::shouldWeModifyCorrespondingJobs, trialsMutator::mutateTrial);
    }


}