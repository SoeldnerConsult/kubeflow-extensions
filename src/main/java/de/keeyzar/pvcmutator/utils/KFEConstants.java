package de.keeyzar.pvcmutator.utils;

/**
 * some often utilized constants, e.g. labels of Kubernetes resources
 */
public class KFEConstants {
    public static final String NOTEBOOK_LABEL="notebook-name";
    public static final String KF_EXTENSION_LABEL="kubeflow-extension";
    public static final String KATIB_CONTAINER_NAME="metrics-logger-and-collector";
    public static final String JOB_NAME_LABEL="job-name";
    public static final String ISTIO_SERVICE_BINDING_NAME_TEMPLATE="bind-ml-pipeline-$namespace";
}
