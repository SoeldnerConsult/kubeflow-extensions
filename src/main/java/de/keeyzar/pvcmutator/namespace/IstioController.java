package de.keeyzar.pvcmutator.namespace;

import de.keeyzar.pvcmutator.pods.KatibPodModifier;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Applicable;
import io.fabric8.kubernetes.client.dsl.Deletable;
import io.fabric8.kubernetes.client.dsl.ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * this class is more or less an istio controller
 * although we utilise yaml files, so we do not need to create the whole nested classed for
 * envoy filter and istio service role binding..
 */
@ApplicationScoped
public class IstioController {
    private static final Logger log = LoggerFactory.getLogger(IstioController.class);

    private final KubernetesClient kubernetesClient;
    private final TemplateProvider templateProvider;


    @Inject
    public IstioController(KubernetesClient kubernetesClient, TemplateProvider templateProvider) {
        this.kubernetesClient = kubernetesClient;
        this.templateProvider = templateProvider;
    }

    public void createIstioServiceRoleBinding(String namespaceName){
        log.info("trying to create new istio ServiceRoleBinding with parameter {}", namespaceName);
        loadAndApplyIstioTemplate(namespaceName, Applicable::createOrReplace);
    }

    public void deleteIstioServiceRoleBinding(String namespaceName){
        log.info("trying to delete new istio ServiceRoleBinding with parameter {}", namespaceName);
        loadAndApplyIstioTemplate(namespaceName, Deletable::delete);
    }

    public void createEnvoyFilterExample(String namespaceName, String profileName){
        String istioServiceRoleBindingTemplate = templateProvider.getEnvoyFilterTemplate();
        String finalText = istioServiceRoleBindingTemplate
                .replaceAll("\\$namespace", namespaceName)
                .replaceAll("\\$userId", profileName);

        loadAndApplyRessource(finalText, Applicable::createOrReplace);
    }

    private void loadAndApplyIstioTemplate(String namespaceName, Consumer<ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean>> whatToDoWithKubernetesResource) {
        String istioServiceRoleBindingTemplate = templateProvider.getIstioServiceRoleBindingTemplate();
        String finalText = istioServiceRoleBindingTemplate.replaceAll("\\$namespace", namespaceName);

        loadAndApplyRessource(finalText, whatToDoWithKubernetesResource);
    }

    private void loadAndApplyRessource(String yamlAsString, Consumer<ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean>> whatToDoWithKubernetesResource) {
        log.info("trying to load following yaml to kubernetesClient {}", yamlAsString);
        InputStream stream = new ByteArrayInputStream(yamlAsString.getBytes(StandardCharsets.UTF_8));
        ParameterNamespaceListVisitFromServerGetDeleteRecreateWaitApplicable<HasMetadata, Boolean> load;

        try {
            load = kubernetesClient.load(stream);
            log.error("successfully loaded yaml string into kubernetes client");
        } catch (Exception e){
            log.error("could not load string {} into kubernetes client!", yamlAsString);
            throw e;
        }

        try {
            whatToDoWithKubernetesResource.accept(load);
        } catch (Exception e){
            log.error("we could not apply the resource to kubernetes api-server");
            throw e;
        }
    }


}
@ApplicationScoped
class TemplateProvider{
    private static final Logger log = LoggerFactory.getLogger(KatibPodModifier.class);

    private String istioServiceRoleBindingTemplate;
    private String envoyFilterTemplate;

    public String getEnvoyFilterTemplate() {
        if(envoyFilterTemplate == null){
            envoyFilterTemplate = getTemplateAsString("EnvoyFilterExample.yaml");
        }
        return envoyFilterTemplate;
    }

    public String getIstioServiceRoleBindingTemplate() {
        if (istioServiceRoleBindingTemplate == null){
            istioServiceRoleBindingTemplate = getTemplateAsString("ServiceRoleBindingExample.yaml");
        }
        return istioServiceRoleBindingTemplate;
    }

    private String getTemplateAsString(String filename) {
        String templateText;
        try {

            templateText = new String(
                    getClass().getClassLoader().getResourceAsStream(filename)
                            .readAllBytes()
            );
        } catch (NullPointerException | IOException e) {
            log.error("Did not find the template file with the name: {}", filename);
            throw new RuntimeException(e);
        }
        return templateText;
    }
}
