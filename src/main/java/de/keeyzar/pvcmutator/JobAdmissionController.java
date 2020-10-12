package de.keeyzar.pvcmutator;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.zjsonpatch.JsonPatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static javax.json.bind.JsonbConfig.FORMATTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/job/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class JobAdmissionController {
    private SharedLists sharedLists;
    private static final Logger log = LoggerFactory.getLogger(JobAdmissionController.class);

    @Inject
    public JobAdmissionController(SharedLists sharedLists){
        this.sharedLists = sharedLists;
    }

    @POST
    public AdmissionReview validate(AdmissionReview review) {

        AdmissionRequest request = review.getRequest();
        if(request.getOperation().equals("CREATE")){
            Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
//            log.info("received create admission review: {}", jsonb.toJson(review));
        }

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource object = request.getObject();

        if (request.getOperation().equals("CREATE") && object instanceof Job) {
            Job job = (Job) object;
            if (doesJobNeedModification(job)) {
//                sharedLists.getJobList().add(job.getSpec().getTemplate().getMetadata().getLabels().get("job-name"));
                JsonObject original = toJsonObject(job);
                mutateJob(job);
                JsonObject mutated = toJsonObject(job);
                cleanupExperimentList(job);

                String patch = Json.createDiff(original, mutated).toString();
                String encoded = Base64.getEncoder().encodeToString(patch.getBytes());
                //encoded change istio inject true; add notebook name: test-kale, set serviceaccount = default-editor
//                encoded = "W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL21ldGFkYXRhL2Fubm90YXRpb25zL3NpZGVjYXIuaXN0aW8uaW9+MWluamVjdCIsInZhbHVlIjoidHJ1ZSJ9LHsib3AiOiJhZGQiLCJwYXRoIjoiL3NwZWMvdGVtcGxhdGUvbWV0YWRhdGEvbGFiZWxzL25vdGVib29rLW5hbWUiLCJ2YWx1ZSI6InRlc3Qta2FsZSJ9LHsib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL3NwZWMvc2VydmljZUFjY291bnROYW1lIiwidmFsdWUiOiJkZWZhdWx0LWVkaXRvciJ9XQ==";
                //encoded: change namespace to "kubeflow"
//                encoded ="W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9tZXRhZGF0YS9uYW1lc3BhY2UvIiwidmFsdWUiOiJrdWJlZmxvdyJ9XQ";
//                encoded="W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL3NwZWMvY29udGFpbmVycy8wL2NvbW1hbmQvMCIsInZhbHVlIjoic2xlZXAgMyAmJiBweXRob24zIC11IC1jIFwiZnJvbSBrYWxlLmNvbW1vbi5rZnB1dGlscyAgICAgICAgICAgICAgICBpbXBvcnQgY3JlYXRlX2FuZF93YWl0X2tmcF9ydW47ICAgICAgICAgICAgICAgIGNyZWF0ZV9hbmRfd2FpdF9rZnBfcnVuKCAgICAgICAgICAgICAgICAgICAgcGlwZWxpbmVfaWQ9JzMyNGEzYTgzLTdmYmUtNGIyYS04OGZjLTMxMWEzMmJhMDYyMicsICAgICAgICAgICAgICAgICAgICBydW5fbmFtZT0nZG9nLWJyZWVkLXdpdGgtd2ViaG9vay1rYXRpYi12NC14YmZuMC1tajg2OXdwbicsICAgICAgICAgICAgICAgICAgICBleHBlcmltZW50X25hbWU9J2RvZy1icmVlZC13aXRoLXdlYmhvb2sta2F0aWItdjQteGJmbjAnLCBub2Rlc19udW1iZXI9JzI1MScsICAgICAgICAgICAgICAgICkgXCIifSx7Im9wIjoicmVwbGFjZSIsInBhdGgiOiIvc3BlYy90ZW1wbGF0ZS9tZXRhZGF0YS9hbm5vdGF0aW9ucy9zaWRlY2FyLmlzdGlvLmlvfjFpbmplY3QiLCJ2YWx1ZSI6InRydWUifSx7Im9wIjoiYWRkIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL21ldGFkYXRhL2xhYmVscy9ub3RlYm9vay1uYW1lIiwidmFsdWUiOiJ0ZXN0LWthbGUifSx7Im9wIjoicmVwbGFjZSIsInBhdGgiOiIvc3BlYy90ZW1wbGF0ZS9zcGVjL3NlcnZpY2VBY2NvdW50TmFtZSIsInZhbHVlIjoiZGVmYXVsdC1lZGl0b3IifSx7Im9wIjoiYWRkIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL21ldGFkYXRhL2xhYmVscy9rdWJlLWV4dGVuc2lvbiIsInZhbHVlIjoidHJ1ZSJ9XQ==";
                log.info("encoded patch is: {}", encoded);
                log.info("patching with {}", patch);

                responseBuilder
                        .withPatchType("JSONPatch")
                        .withPatch(encoded);
            }

        }
        if(request.getOperation().equals("UPDATE")){
            Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
            log.info("received update admission review: {}", jsonb.toJson(review));
        }

        //fix necessary, because we can't fix admissionReview of AdmissionReviewBuilder
        //and we can't set v1beta1 in kubernetes
        AdmissionReview admissionReview = new AdmissionReviewBuilder().withResponse(responseBuilder.build()).build();
        admissionReview.setApiVersion("admission.k8s.io/v1");
        return admissionReview;
    }

    private void cleanupExperimentList(Job job) {
        sharedLists.getTrialList().remove(job.getMetadata().getName());
    }

    JsonObject toJsonObject(Job trial) {

        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(trial))).readObject();
    }

    boolean doesJobNeedModification(Job job) {
        return true;
//        log.info("Checking whether or not a modification of the job is necessary");
//        log.info("{} is the name of the job", job.getMetadata().getName());
//        sharedLists.getTrialList().forEach((e) -> log.info("jobs to be mutated: {}", e ));
//        return sharedLists.getTrialList().contains(job.getMetadata().getName());
    }

    void mutateJob(Job job) {
        PodTemplateSpec template = job.getSpec().getTemplate();
        log.info("mutating job!");
        template.getMetadata().getLabels().put("kubeflow-extension", "true");
        //todo find a better way for this to work :) (maybe another envoyfilter for kube-extension
        template.getMetadata().getLabels().put("notebook-name", "test-kale");

        //todo ~1 is for escaping / in path.. this may be done post patch creating, if it's not working
        template.getMetadata().getAnnotations().put("sidecar.istio.io~1inject", "true");
        template.getSpec().setServiceAccountName("default-editor");
        List<String> command = template.getSpec().getContainers().get(0).getCommand();
        String newCommand = "sleep && " + String.join(" ", command);
        template.getSpec().getContainers().get(0).setCommand(List.of(newCommand));

        //this is a huge annoying error.. slash as a key will
        //not work in json patch; we need to fix this

//        template.getMetadata().getLabels().put("notebook-name", "test-kale");

//        job.getMetadata().setNamespace("kubeflow");
    }
}