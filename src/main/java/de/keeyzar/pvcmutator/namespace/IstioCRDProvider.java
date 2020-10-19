package de.keeyzar.pvcmutator.namespace;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IstioCRDProvider {
    private final CustomResourceDefinitionContext serviceRoleBindingCRDContext;
    private final CustomResourceDefinitionContext envoyCRDContext;

    public IstioCRDProvider(){
        CustomResourceDefinitionContext.Builder builder = new CustomResourceDefinitionContext.Builder();
        serviceRoleBindingCRDContext = builder.withGroup("rbac.istio.io")
                .withScope("Namespaced")
                .withKind("ServiceRoleBinding")
                .withPlural("servicerolebindings")
                .withVersion("v1alpha1")
                .build();

        builder = new CustomResourceDefinitionContext.Builder();
        envoyCRDContext = builder.withGroup("networking.istio.io")
                .withScope("Namespaced")
                .withKind("EnvoyFilter")
                .withPlural("envoyfilters")
                .withVersion("v1alpha3")
                .build();
    }

    public CustomResourceDefinitionContext getServiceRoleBindingCRDContext(){
        return serviceRoleBindingCRDContext;
    }

    public CustomResourceDefinitionContext getEnvoyFilterCRDContext() {
        return envoyCRDContext;
    }
}
