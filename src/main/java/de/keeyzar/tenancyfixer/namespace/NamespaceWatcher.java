package de.keeyzar.tenancyfixer.namespace;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * watches namespace creation
 */
@ApplicationScoped
public class NamespaceWatcher {
    private static final Logger log = LoggerFactory.getLogger(IstioController.class);

    private final KubernetesClient kubernetesClient;

    @Inject
    public NamespaceWatcher(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public void registerNamespaceListener(Type operationType, Consumer<Namespace> listener){
        SharedInformerFactory informers = kubernetesClient.informers();
        SharedIndexInformer<Namespace> namespaceSharedIndexInformer =
                informers.sharedIndexInformerFor(Namespace.class, NamespaceList.class, 1000);
        namespaceSharedIndexInformer.addEventHandler(new ResourceEventHandler<>() {
            @Override
            public void onAdd(Namespace addedNamespace) {
                log.info("new namespace: {} registered operation: {}", addedNamespace.getMetadata().getName(), operationType);
                if(Type.CREATE == operationType)
                    listener.accept(addedNamespace);
            }

            @Override
            public void onUpdate(Namespace oldObj, Namespace newObj) {
                //don't do anything
            }

            @Override
            public void onDelete(Namespace deletedNamespace, boolean deletedFinalStateUnknown) {
                log.info("deleted namespace: {} registered operation: {}", deletedNamespace.getMetadata().getName(), operationType);
                if(Type.DELETE == operationType)
                    listener.accept(deletedNamespace);
            }
        });
        informers.startAllRegisteredInformers();
    }

    public enum Type{
        CREATE,
        DELETE
    }
}
