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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.io.StringReader;
import java.util.Base64;

import static javax.json.bind.JsonbConfig.FORMATTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/trial/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TrialsAdmissionController {
    private static final Logger log = LoggerFactory.getLogger(TrialsAdmissionController.class);

    @Inject
    public TrialsAdmissionController(){

    }

    public void preRegister(@Observes StartupEvent startupEvent){
        KubernetesDeserializer.registerCustomKind("kubeflow.org/v1alpha3", "Trial", Trial.class);
    }



    @POST
    public AdmissionReview validate(AdmissionReview review) {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
        //todo print some fkn stuff, when on debug

        AdmissionRequest request = review.getRequest();

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource object = request.getObject();

        if (request.getOperation().equals("CREATE") && object instanceof Trial) {
            Trial trial = (Trial) object;
            if (shouldWeModifyCorrespondingJobs(trial)) {

                JsonObject original = toJsonObject(trial);
                mutateTrial(trial);
                JsonObject mutated = toJsonObject(trial);

                String patch = Json.createDiff(original, mutated).toString();
                String encoded = Base64.getEncoder().encodeToString(patch.getBytes());

                log.info("encoded patch is: {}", encoded);
                log.info("patching with {}", patch);

                responseBuilder
                        .withPatchType("JSONPatch")
                        .withPatch(encoded);
            }

        }

        //fix necessary, because we can't fix admissionReview of AdmissionReviewBuilder
        //and we can't set v1beta1 in kubernetes
        AdmissionReview admissionReview = new AdmissionReviewBuilder().withResponse(responseBuilder.build()).build();
        admissionReview.setApiVersion("admission.k8s.io/v1");
        return admissionReview;
    }

    private void mutateTrial(Trial trial) {
        trial.getMetadata().getLabels().put("kubeflow-extension", "true");
    }


    JsonObject toJsonObject(Trial trial) {
        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(trial))).readObject();
    }


    boolean shouldWeModifyCorrespondingJobs(Trial trial) {
        //each trial not in kubeflow will have connection problems...
        boolean shouldModifyCorrespondingJobs = !"kubeflow".equals(trial.getMetadata().getNamespace());
        log.info("Should we modify corresponding jobs? {}", shouldModifyCorrespondingJobs);
        return shouldModifyCorrespondingJobs;
    }
}