package de.keeyzar.pvcmutator.namespace;

import de.keeyzar.pvcmutator.pojo.profile.Profile;
import io.fabric8.kubernetes.api.model.Namespace;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Optional;

@ApplicationScoped
public class Controller {
    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    private final NamespaceWatcher namespaceWatcher;
    private final ProfileController profileController;
    private final IstioController istioController;

    @Inject
    public Controller(NamespaceWatcher namespaceWatcher, ProfileController profileController,
                      IstioController istioController) {
        this.namespaceWatcher = namespaceWatcher;
        this.profileController = profileController;
        this.istioController = istioController;
    }

    void onStart(@Observes StartupEvent ev) {
        initializeNamespaceWatching();
    }

    private void initializeNamespaceWatching() {
        namespaceWatcher.registerNamespaceListener(NamespaceWatcher.Type.CREATE,
                this::handleNewNamespace);

        namespaceWatcher.registerNamespaceListener(NamespaceWatcher.Type.DELETE,
                this::handleDeletedNamespace);
    }

    private void handleDeletedNamespace(Namespace namespace) {
        istioController.deleteIstioServiceRoleBinding(namespace.getMetadata().getName());
    }

    private void handleNewNamespace(Namespace newNamespace) {
        String namespaceName= newNamespace.getMetadata().getName();
        Optional<Profile> profileOptional = profileController.getProfile(namespaceName);
        if(profileOptional.isPresent()){
            log.info("We found a profile, proceeding with opening access to ml-pipelines");
            Profile profile = profileOptional.get();
            String owner = profile.getSpec().getOwner().getName();
            istioController.createIstioServiceRoleBinding(namespaceName);
            istioController.createEnvoyFilterExample(namespaceName, owner);
        } else {
            log.info("We did not find a profile, therefore we don't do anything!");
        }
    }


}
