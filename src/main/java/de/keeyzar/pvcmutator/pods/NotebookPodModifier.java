package de.keeyzar.pvcmutator.pods;

import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

import static de.keeyzar.pvcmutator.utils.KFEConstants.KF_EXTENSION_LABEL;
import static de.keeyzar.pvcmutator.utils.KFEConstants.NOTEBOOK_LABEL;

@ApplicationScoped
public class NotebookPodModifier {
    private static final Logger log = LoggerFactory.getLogger(KatibPodModifier.class);

    void modifyNotebookPod(Pod pod) {
        log.info("modification of notebook pod, appending label {} = {}", KF_EXTENSION_LABEL, "true");
        pod.getMetadata().getLabels().put(KF_EXTENSION_LABEL, "true");
    }

    boolean isModificationNecessary(Pod pod) {
        boolean isNotebookPod = pod.getMetadata().getLabels().containsKey(NOTEBOOK_LABEL);
        log.info("is this pod a notebook pod?: {}", isNotebookPod);
        return isNotebookPod;
    }

}
