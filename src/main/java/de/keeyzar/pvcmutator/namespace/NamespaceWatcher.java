package de.keeyzar.pvcmutator.namespace;

import io.fabric8.kubernetes.api.model.Namespace;

import javax.enterprise.context.ApplicationScoped;
import java.util.function.Consumer;

/**
 * watches namespace creation
 */
@ApplicationScoped
public class NamespaceWatcher {
    public void registerNamespaceListener(Type operationType, Consumer<Namespace> listener){

    }

    public enum Type{
        CREATE,
        DELETE
    }
}
