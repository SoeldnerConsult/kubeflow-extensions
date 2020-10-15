package de.keeyzar.pvcmutator.pods;

import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.keeyzar.pvcmutator.utils.KFEConstants.KF_EXTENSION_LABEL;

@ApplicationScoped
public class KatibPodModifier {
    private static final Logger log = LoggerFactory.getLogger(KatibPodModifier.class);

    /**
     * this method is necessary, as the pod has an empty list already attached,
     * therefore the created json patch will have errors!
     */
    void removeCommandEmptyListIfPresent(Pod pod) {
        Container katibContainer = findMainContainer(pod);
        if(katibContainer.getCommand().isEmpty()) {
            katibContainer.setCommand(null);
        }
    }

    boolean isModificationNecessary(Pod pod) {
        log.debug("pod.meta.labels = {}", pod.getMetadata().getLabels());
        log.debug("pod.label contains?? {}", pod.getMetadata().getLabels().containsKey(KF_EXTENSION_LABEL));
        pod.getMetadata().getLabels().forEach((k, v) -> log.trace("key: {} value{}", k, v));
        return pod.getMetadata().getLabels().containsKey(KF_EXTENSION_LABEL);
    }

    void mutatePod(Pod pod) {
        log.info("starting to mutate pod");
        Container katibContainer = findContainerByName(pod, KFEConstants.KATIB_CONTAINER_NAME);
        Container mainContainer = findMainContainer(pod);

        setIstioLabelToTrue(pod);
        fixMainContainerArgsAndCommands(mainContainer);
        fixKatibContainerArgsAndCommand(katibContainer);

        log.info("pod mutation finished");
    }

    private void setIstioLabelToTrue(Pod pod) {
        //when patching this path we need to jsonPatch escape the slash in istio.io/inject with ~1
        pod.getMetadata().getAnnotations().put("sidecar.istio.io~1inject", "true");
    }

    private void fixMainContainerArgsAndCommands(Container mainContainer) {
        addSleepBeforeFirstArg(mainContainer);
    }

    private void addSleepBeforeFirstArg(Container mainContainer) {
        List<String> args = mainContainer.getArgs();
        args.add(0, "sleep 3 &&");
        String newLongArg = String.join(" ", args);
        mainContainer.setArgs(List.of(newLongArg));
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

    private Container findMainContainer(Pod pod) {
        String jobName = pod.getMetadata().getLabels().get(KFEConstants.JOB_NAME_LABEL);
        if(jobName == null){
            //TODO
            throw new RuntimeException("better errorhandling necessary");
        }
        return findContainerByName(pod, jobName);
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

        //this call will fail, if no container is found, therefore find a good
        //exception handling strategy
        return collect.get(0);
    }

}
