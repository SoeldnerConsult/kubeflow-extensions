package de.keeyzar.pvcmutator;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static javax.json.bind.JsonbConfig.FORMATTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/pod/mutate")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class PodAdmissionController {
    private static final Logger log = LoggerFactory.getLogger(PodAdmissionController.class);

    @POST
    public AdmissionReview validate(AdmissionReview review) {

        AdmissionRequest request = review.getRequest();
        if(request.getOperation().equals("CREATE")){
            Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
            log.info("received pod create admission review: {}", jsonb.toJson(review));
        }

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource object = request.getObject();

        if (request.getOperation().equals("CREATE") && object instanceof Pod) {
            Pod pod = (Pod) object;
            log.info("do we need to modify pod?");
            if (doesJobNeedModification(pod)) {
                log.info("yes, we need to modify pod! :)");
                removeCommandEmptyListIfPresent(pod);
                JsonObject original = toJsonObject(pod);


                mutateJob(pod);
                JsonObject mutated = toJsonObject(pod);

                String patch = Json.createDiff(original, mutated).toString();

                fixJsonPatchError(patch);
                String encoded = Base64.getEncoder().encodeToString(patch.getBytes());
                //encoded change istio inject true; add notebook name: test-kale, set serviceaccount = default-editor
//                encoded = "W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL21ldGFkYXRhL2Fubm90YXRpb25zL3NpZGVjYXIuaXN0aW8uaW9+MWluamVjdCIsInZhbHVlIjoidHJ1ZSJ9LHsib3AiOiJhZGQiLCJwYXRoIjoiL3NwZWMvdGVtcGxhdGUvbWV0YWRhdGEvbGFiZWxzL25vdGVib29rLW5hbWUiLCJ2YWx1ZSI6InRlc3Qta2FsZSJ9LHsib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL3NwZWMvc2VydmljZUFjY291bnROYW1lIiwidmFsdWUiOiJkZWZhdWx0LWVkaXRvciJ9XQ==";
                //encoded: change namespace to "kubeflow"
//                encoded ="W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9tZXRhZGF0YS9uYW1lc3BhY2UvIiwidmFsdWUiOiJrdWJlZmxvdyJ9XQ";
                //das ist replace mit nohup kill am ende, funktional, nur bringt suprocess nichts :=)
//                encoded="W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL2NvbnRhaW5lcnMvMC9hcmdzLzAiLCJ2YWx1ZSI6InNsZWVwIDMgJiYgcHl0aG9uMyAtdSAtYyBcImZyb20ga2FsZS5jb21tb24ua2ZwdXRpbHMgICAgICAgICAgICAgICAgaW1wb3J0IGNyZWF0ZV9hbmRfd2FpdF9rZnBfcnVuOyAgICAgICAgICAgICAgICBjcmVhdGVfYW5kX3dhaXRfa2ZwX3J1biggICAgICAgICAgICAgICAgICAgIHBpcGVsaW5lX2lkPSczMjRhM2E4My03ZmJlLTRiMmEtODhmYy0zMTFhMzJiYTA2MjInLCAgICAgICAgICAgICAgICAgICAgcnVuX25hbWU9J2RvZy1icmVlZC13aXRoLXdlYmhvb2sta2F0aWItdjQteGJmbjAtbWo4Njl3cG4nLCAgICAgICAgICAgICAgICAgICAgZXhwZXJpbWVudF9uYW1lPSdkb2ctYnJlZWQtd2l0aC13ZWJob29rLWthdGliLXY0LXhiZm4wJywgbm9kZXNfbnVtYmVyPScyNTEnLCAgICAgICAgICAgICAgICApIFwiIDE+L3Zhci9sb2cva2F0aWIvbWV0cmljcy5sb2cgMj4mMSAmJiBlY2hvIGNvbXBsZXRlZCA+IC92YXIvbG9nL2thdGliLyQkJCQucGlkIDsgZXhpdF9jb2RlPSQ/OyBub2h1cCBzbGVlcCAxMCAmJiBwa2lsbCAtZiAvdXNyL2xvY2FsL2Jpbi9waWxvdC1hZ2VudCAmIGV4aXQgJGV4aXRfY29kZTsifV0=";
                //das ist add an arg on katib container
                //das funktioniert aber auch nicht
//                encoded="W3sib3AiOiJhZGQiLCJwYXRoIjoiL3NwZWMvY29udGFpbmVycy8yL2FyZ3MvLSIsInZhbHVlIjoiJiYgcGtpbGwgLWYgL3Vzci9sb2NhbC9iaW4vcGlsb3QtYWdlbnQifV0=";
                //das hier macht aus katib args => command sh -c "filemetrics.. ;pkill" und das funktioniert! :)
//                encoded="WwogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzciCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzYiCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzUiCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzQiCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzMiCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzIiCiB9LAogewogICJvcCI6ICJyZW1vdmUiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9hcmdzLzEiCiB9LAogewogICJvcCI6ICJyZXBsYWNlIiwKICAicGF0aCI6ICIvc3BlYy9jb250YWluZXJzLzIvYXJncy8wIiwKICAidmFsdWUiOiAiLi9maWxlLW1ldHJpY3Njb2xsZWN0b3IgLXQgZG9nLWJyZWVkLXdpdGgtd2ViaG9vay1rYXRpYi12NC1raXo4Ni04OXBwODJ2cCAtbSB0ZXN0LWFjY3VyYWN5LXJlc25ldCAtcyBrYXRpYi1kYi1tYW5hZ2VyLmt1YmVmbG93OjY3ODkgLXBhdGggL3Zhci9sb2cva2F0aWIvbWV0cmljcy5sb2c7cGtpbGwgLWYgL3Vzci9sb2NhbC9iaW4vcGlsb3QtYWdlbnQiCiB9LAogewogICJvcCI6ICJhZGQiLAogICJwYXRoIjogIi9zcGVjL2NvbnRhaW5lcnMvMi9jb21tYW5kIiwKICAidmFsdWUiOiBbCiAgICJzaCIsCiAgICItYyIKICBdCiB9Cl0=";
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

    /**
     * this method is necessary, as the pod has an empty list already attached,
     * therefore the created json patch will have errors!
     * @param pod
     */
    private void removeCommandEmptyListIfPresent(Pod pod) {
        Container katibContainer = findKatibContainer(pod);
        if(katibContainer.getCommand().isEmpty()) {
            katibContainer.setCommand(null);
        }
    }

    private void fixJsonPatchError(String patch) {

    }

    JsonObject toJsonObject(Pod pod) {
        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(pod))).readObject();
    }

    boolean doesJobNeedModification(Pod pod) {
        log.trace("pod.meta.labels = {}", pod.getMetadata().getLabels());
        log.trace("pod.label contains?? {}", pod.getMetadata().getLabels().containsKey("kubeflow-extension"));
        pod.getMetadata().getLabels().forEach((k, v) -> log.trace("key: {} value{}", k, v));
        return pod.getMetadata().getLabels().containsKey("kubeflow-extension");
//        return sharedLists.getJobList().remove(pod.getMetadata().getLabels().get("job-name"));
//        log.info("Checking whether or not a modification of the job is necessary");
//        log.info("{} is the name of the job", job.getMetadata().getName());
//        sharedLists.getTrialList().forEach((e) -> log.info("jobs to be mutated: {}", e ));
//        return sharedLists.getTrialList().contains(job.getMetadata().getName());
    }

    void mutateJob(Pod pod) {
        log.info("starting to mutate pod");
        Container container = findKatibContainer(pod);
        fixKatibContainerArgsAndCommand(container);
        log.info("pod mutation finished");
    }

    private void fixKatibContainerArgsAndCommand(Container container) {
        List<String> args = collectAllArgs(container);
        args.add(0, "./file-metricscollector");
        args.add(";" +
                "exit_code=$?;" +
                "sleep 5;" +
                "cp /dev/termination-log /dev/termination-log.bak;" +
                "pkill -INT /usr/local/bin/pilot-agent;" +
                "sleep 10;" +
                "echo '' > /dev/termination-log;" +
                "exit $exit_code;");
        String singleArg = String.join(" ", args);
        args.clear();
        args.add(singleArg);
        if(container.getCommand() == null){
            container.setCommand(new ArrayList<>());
        }
        container.getCommand().clear();
        container.getCommand().addAll(List.of("sh", "-c"));
    }

    private List<String> collectAllArgs(Container container) {
        if(container.getArgs().isEmpty()){
            log.error("The container does not have any args, please fix this issue!");
        }
        return container.getArgs();
    }

    private Container findKatibContainer(Pod pod) {
        return findContainerByName(pod, "metrics-logger-and-collector");
    }



    private Container findContainerByName(Pod pod, String containerNameToSearch){
        log.info("Searching for container with name {} in Pod", containerNameToSearch);

        List<Container> containers = pod.getSpec().getContainers();
        List<Container> collect = containers
                .stream()
                .filter((e) -> containerNameToSearch.equals(e.getName()))
                .collect(Collectors.toList());

        boolean error= false;
        if(collect.isEmpty()){
            log.error("We did not find a container.. this is a major problem");
            error=true;
        } else if (collect.size() > 1){
            log.error("we found more than one container with the following name: {}", containerNameToSearch);
            error=true;
        }
        if(error){
            log.error("All possible containers:");
            containers.forEach((e) -> log.error("Containername: {}", e.getName()));
            //todo better errorhandling
        }

        return collect.get(0);
    }
}