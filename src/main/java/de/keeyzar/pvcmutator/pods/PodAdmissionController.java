package de.keeyzar.pvcmutator.pods;

import de.keeyzar.pvcmutator.utils.AdmissionReviewMutatorHelper;
import de.keeyzar.pvcmutator.utils.AdmissionReviewMutatorHelper.ReviewHook;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/pod/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)

public class PodAdmissionController {
    private static final Logger log = LoggerFactory.getLogger(PodAdmissionController.class);
    private final NotebookPodModifier notebookPodModifier;
    private final KatibPodModifier katibPodModifier;
    private final AdmissionReviewMutatorHelper admissionReviewMutatorHelper;

    @Inject
    public PodAdmissionController(NotebookPodModifier notebookPodModifier,
                                  KatibPodModifier katibPodModifier,
                                  AdmissionReviewMutatorHelper admissionReviewMutatorHelper){
        this.notebookPodModifier = notebookPodModifier;
        this.katibPodModifier = katibPodModifier;
        this.admissionReviewMutatorHelper = admissionReviewMutatorHelper;
    }

    @POST
    public AdmissionReview mutate(AdmissionReview review) {
        log.info("received Pod AdmissionReview");
        ReviewHook<Pod> reviewHookNotebookPod = new ReviewHook<>(notebookPodModifier::isModificationNecessary,
                notebookPodModifier::modifyNotebookPod, Optional.empty());
        ReviewHook<Pod> reviewHookKatibPod = new ReviewHook<>(katibPodModifier::isModificationNecessary,
                katibPodModifier::mutatePod, Optional.of(katibPodModifier::removeCommandEmptyListIfPresent));

        return admissionReviewMutatorHelper.createAdmissionReview(review,
                Pod.class, List.of(reviewHookNotebookPod, reviewHookKatibPod));
    }
}