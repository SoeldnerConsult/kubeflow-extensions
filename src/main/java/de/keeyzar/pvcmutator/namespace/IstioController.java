package de.keeyzar.pvcmutator.namespace;

import de.keeyzar.pvcmutator.pods.KatibPodModifier;
import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.internal.RawCustomResourceOperationsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
    private final int KUBERNETES_RESOURCE_ALREADY_EXISTS = 409;
    private final RawCustomResourceOperationsImpl istioOp;
    private final RawCustomResourceOperationsImpl envoyOp;

    @Inject
    public IstioController(KubernetesClient kubernetesClient, TemplateProvider templateProvider,
                           IstioCRDProvider istioCRDProvider) {
        this.kubernetesClient = kubernetesClient;
        this.templateProvider = templateProvider;
        istioOp = kubernetesClient.customResource(istioCRDProvider.getServiceRoleBindingCRDContext());
        envoyOp = kubernetesClient.customResource(istioCRDProvider.getEnvoyFilterCRDContext());
    }

    public void createIstioServiceRoleBinding(String namespaceName) {
        log.info("trying to create new istio ServiceRoleBinding with parameter {}", namespaceName);
        loadAndApplyIstioTemplate(namespaceName);
    }

    public void deleteIstioServiceRoleBinding(String namespaceName) {
        String resourceName = createIstioResourceName(namespaceName);
        log.info("trying to delete istio ServiceRoleBinding with name {}", resourceName);

        try {
            istioOp.delete("kubeflow", resourceName);
        } catch (IOException e) {
            log.error("While deleting the istio service role binding an IOException occurred", e);
        }
    }

    public void createEnvoyFilterExample(String namespaceName, String profileName) {
        String envoyFilterTemplate = templateProvider.getEnvoyFilterTemplate();
        String finalText = envoyFilterTemplate
                .replaceAll("\\$namespace", namespaceName)
                .replaceAll("\\$userId", profileName);

        log.info("trying to apply template:\n{}", finalText);
        try (InputStream stream = new ByteArrayInputStream(finalText.getBytes(StandardCharsets.UTF_8))) {
            envoyOp.create(namespaceName, stream);
            log.info("the template is applied!");
        }catch(IOException e) {
            log.error("an IO Error occurred, while reading the template string", e);
        } catch (KubernetesClientException kce){
            if (KUBERNETES_RESOURCE_ALREADY_EXISTS == kce.getCode()) {
                log.info("the EnvoyFilter already exists - skipping!");
            } else {
                log.error("while applying the resource, a KubernetesException occured", kce);
                throw kce;
            }
        }
    }

    private void loadAndApplyIstioTemplate(String namespaceName) {
        String istioServiceRoleBindingTemplate = templateProvider.getIstioServiceRoleBindingTemplate();
        String istioResourceName = createIstioResourceName(namespaceName);
        String finalText = istioServiceRoleBindingTemplate
                .replaceAll("\\$namespace", namespaceName)
                .replaceAll("\\$name", istioResourceName);

        log.info("trying to apply template:\n{}", finalText);
        try (InputStream stream = new ByteArrayInputStream(finalText.getBytes(StandardCharsets.UTF_8))) {
            istioOp.create("kubeflow", stream);
            log.info("the template is applied!");
        }catch(IOException e) {
            log.error("an IO Error occurred, while reading the template string", e);
        } catch (KubernetesClientException kce){
            if (KUBERNETES_RESOURCE_ALREADY_EXISTS == kce.getCode()) {
                log.info("the ServiceRoleBinding already exists - skipping!");
            } else {
                log.error("while applying the resource, a KubernetesException occured", kce);
                throw kce;
            }
        }
    }

    /**
     * obtain name template and replace the placeholder
     */
    private String createIstioResourceName(String namespaceName) {
        return KFEConstants.ISTIO_SERVICE_BINDING_NAME_TEMPLATE.replaceAll("\\$namespace", namespaceName);
    }

}

@ApplicationScoped
class TemplateProvider {
    private static final Logger log = LoggerFactory.getLogger(KatibPodModifier.class);

    private String istioServiceRoleBindingTemplate;
    private String envoyFilterTemplate;

    public String getEnvoyFilterTemplate() {
        if (envoyFilterTemplate == null) {
            envoyFilterTemplate = getTemplateAsString("EnvoyFilterExample.yaml");
        }
        return envoyFilterTemplate;
    }

    public String getIstioServiceRoleBindingTemplate() {
        if (istioServiceRoleBindingTemplate == null) {
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
