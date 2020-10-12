package de.keeyzar.pvcmutator;

import de.keeyzar.pvcmutator.pojo.Trials.Trial;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.json.bind.JsonbConfig.FORMATTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/trial/validate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TrialsAdmissionController {
    private SharedLists sharedLists;
    private static final Logger log = LoggerFactory.getLogger(TrialsAdmissionController.class);

    @Inject
    public TrialsAdmissionController(SharedLists sharedLists){
        this.sharedLists = sharedLists;
    }

    public void preRegister(@Observes StartupEvent startupEvent){
        KubernetesDeserializer.registerCustomKind("kubeflow.org/v1alpha3", "Trial", Trial.class);
    }



    @POST
    public AdmissionReview validate(AdmissionReview review) {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
//        log.info("received admission review: {}", jsonb.toJson(review));

        AdmissionRequest request = review.getRequest();

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource object = request.getObject();

        if (request.getOperation().equals("CREATE") && object instanceof Trial) {
            Trial trial = (Trial) object;
            if (shouldModifyCorrespondingJobs(trial)) {
                saveTrialName(trial);
            }

        }

        //fix necessary, because we can't fix admissionReview of AdmissionReviewBuilder
        //and we can't set v1beta1 in kubernetes
        AdmissionReview admissionReview = new AdmissionReviewBuilder().withResponse(responseBuilder.build()).build();
        admissionReview.setApiVersion("admission.k8s.io/v1");
        return admissionReview;
    }

    private void saveTrialName(Trial trial) {
        sharedLists.getTrialList().add(trial.getMetadata().getName());
        log.info("We save the trial: {}", trial.getMetadata().getName());
    }

    boolean shouldModifyCorrespondingJobs(Trial trial) {
        boolean shouldModifyCorrespondingJobs = !"kubeflow".equals(trial.getMetadata().getNamespace());
        log.info("Should we modify corresponding jobs? {}", shouldModifyCorrespondingJobs);

        return shouldModifyCorrespondingJobs;
    }

//    void mutate(Trial trial) {
//         String string = (String) trial.getSpec()
//                .getAdditionalProperties()
//                .get("runSpec");
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        string = string.replace("sidecar.istio.io/inject: \"false\"", "sidecar.istio.io/inject: \"true\"");
//        string = string.replace("serviceAccountName: pipeline-runner", "serviceAccountName: default-editor");
//        trial.getSpec().getAdditionalProperties().put("runSpec", string);
//        //todo fix namespace envoyfilter
//        String namespace= trial.getMetadata().getNamespace();
//    }
}