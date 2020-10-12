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
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));

        log.debug("Received AdmissionReview: {}", jsonb.toJson(review));

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource requestObject = request.getObject();

        if (request.getOperation().equals("CREATE") && requestObject instanceof Pod) {
            Pod pod = (Pod) requestObject;
            if (doesJobNeedModification(pod)) {

                //the pod is deserialized with an empty list;
                //this results in a incorrect json patch, when a new command is added
                //therefore, delete it when it's empty, because we later must append something
                removeCommandEmptyListIfPresent(pod);

                JsonObject original = toJsonObject(pod);
                mutateJob(pod);
                JsonObject mutated = toJsonObject(pod);

                String patch = Json.createDiff(original, mutated).toString();
                String encoded = Base64.getEncoder().encodeToString(patch.getBytes());

                log.info("patching with patch as json{}", patch);
                log.info("patch base64 encoded: ");

                responseBuilder
                        .withPatchType("JSONPatch")
                        .withPatch(encoded);
            }
        }

        //fix necessary, because we can't fix admissionReview of AdmissionReviewBuilder
        //and we can't set v1beta1 in Kubernetes resource (it's quietly overridden)
        AdmissionReview admissionReview = new AdmissionReviewBuilder().withResponse(responseBuilder.build()).build();
        admissionReview.setApiVersion("admission.k8s.io/v1");
        return admissionReview;
    }

    /**
     * this method is necessary, as the pod has an empty list already attached,
     * therefore the created json patch will have errors!
     */
    private void removeCommandEmptyListIfPresent(Pod pod) {
        Container katibContainer = findKatibContainer(pod);
        if(katibContainer.getCommand().isEmpty()) {
            katibContainer.setCommand(null);
        }
    }

    boolean doesJobNeedModification(Pod pod) {
        log.debug("pod.meta.labels = {}", pod.getMetadata().getLabels());
        log.debug("pod.label contains?? {}", pod.getMetadata().getLabels().containsKey("kubeflow-extension"));
        pod.getMetadata().getLabels().forEach((k, v) -> log.trace("key: {} value{}", k, v));
        return pod.getMetadata().getLabels().containsKey("kubeflow-extension");
    }

    void mutateJob(Pod pod) {
        log.info("starting to mutate pod");
        Container container = findKatibContainer(pod);
        fixKatibContainerArgsAndCommand(container);
        log.info("pod mutation finished");
    }

    /**
     * we override the following behaviour of the katib container
     * cmd ./file-metricscollector
     * args -arg 1
     *      -arg 2...
     * and we create
     *
     * cmd sh -c
     * args - "./file-metricscollector; kill istio; exit"
     *
     *
     * the reason for that is
     * so we can kill the istio pod *after* metrics collection
     * we can't append on the original experiment container, because
     * the katib container waits for the destruction
     * of the original experiment container pod..
     * killing istio in this stage will *always* result in a failed
     * job, because katib could not send through the istio proxy.
     */
    private void fixKatibContainerArgsAndCommand(Container container) {
        if(container.getCommand() == null){
            container.setCommand(new ArrayList<>());
        }
        container.getCommand().clear();
        container.getCommand().addAll(List.of("sh", "-c"));


        List<String> args = collectAllArgs(container);
        //the ./file-metricscollector command was hidden in the
        //entrypoint docker command; now explicitly necessary, as we wrap it
        //within the sh -c ".." command
        args.add(0, "./file-metricscollector");

        //add a bit of bash code;
        //istio pod won't kill itself as a sidecar
        //therefore job won't finish..
        //killing with pkill unfortunately safes the exit code in termination file, even though
        //we ignore the exit code in this shell script
        //we need to ignore it within the command and override the termination-log
        //or the job will finish with a failed state
        args.add(";" +
                "exit_code=$?;" + //safe exit code
                "sleep 5;" + //safety sleep (is this necessary) TODO check
                "cat /dev/termination-log > /dev/termination-log.bak;" + //cp + mv not working.. someone is watching this file
                "pkill -INT /usr/local/bin/pilot-agent;" + //kill istio sidecar
                "sleep 5;" + //safety sleep (is this necessary?) TODO check
                "cat /dev/termination-log.bak > /dev/termination-log;" + //restore original termination log
                "exit $exit_code;"); //exit with exit code of file-metricscollector

        //we need a single arg, or kubernetes
        //will create something like this:
        //sh -c "command a" "command b" therefore it won't work.
        String singleArg = String.join(" ", args);
        args.clear();
        args.add(singleArg);
    }

    private List<String> collectAllArgs(Container container) {
        if(container.getArgs().isEmpty()){
            log.error("The container does not have any args, please fix this issue!");
            //todo better ErrorHandling
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

    JsonObject toJsonObject(Pod pod) {
        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(pod))).readObject();
    }
}