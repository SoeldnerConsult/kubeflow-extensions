package de.keeyzar.tenancyfixer.pods;

import de.keeyzar.tenancyfixer.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.keeyzar.tenancyfixer.utils.KFEConstants.KF_EXTENSION_LABEL;

@ApplicationScoped
public class KatibPodModifier {
    private static final Logger log = LoggerFactory.getLogger(KatibPodModifier.class);

    /**
     * this method is necessary, as the pod has an empty list already attached,
     * therefore the created json patch will have errors!
     */
    void removeCommandEmptyListIfPresent(Pod pod) {
        log.info("pre modification hook for katib container will be applied");
        Container katibContainer = findContainerByName(pod, KFEConstants.KATIB_CONTAINER_NAME);
        if(katibContainer.getCommand().isEmpty()) {
            log.info("container commands where empty, therefore set the container null!");
            katibContainer.setCommand(null);
        }
    }

    boolean isModificationNecessary(Pod pod) {
        boolean containsLabel = pod.getMetadata().getLabels().containsKey(KF_EXTENSION_LABEL);
        log.info("is this pod a katib pod? = {}", containsLabel);

        if(!containsLabel){
            log.info("label: {} not found. All available labels:", KF_EXTENSION_LABEL);
            pod.getMetadata().getLabels().forEach((k, v) -> log.info("key: {} value: {}", k, v));
        }

        return containsLabel;
    }

    void mutatePod(Pod pod) {
        log.info("starting to mutate pod");
        Container katibContainer = findContainerByName(pod, KFEConstants.KATIB_CONTAINER_NAME);
        Container mainContainer = findMainContainer(pod);

        fixMainContainerArgsAndCommands(mainContainer);
        fixKatibContainerArgsAndCommand(katibContainer);

        log.info("pod mutation finished");
    }

    private void fixMainContainerArgsAndCommands(Container mainContainer) {
        addSleepBeforeFirstArg(mainContainer);
    }

    private void addSleepBeforeFirstArg(Container mainContainer) {
        List<String> args = mainContainer.getArgs();
        //1 second sleep is not enough
        //we get a connection refused error on each pod
        // ; 3 did not fail that often, but when we
        //to have a time dependency better be sure than sorry..
        //yeah.. there are better ways.. but at what cost?
        args.add(0, "sleep 5 &&");
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
                "cat /dev/termination-log > /dev/termination-log.bak;" + //cp + mv not working.. someone is watching this file
                "pkill -INT /usr/local/bin/pilot-agent;" + //kill istio sidecar
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
            //TODO better ErrorHandling
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
            //todo better ErrorHandling
        }

        //this call will fail, if no container is found, therefore find a good
        //exception handling strategy
        return collect.get(0);
    }

}
