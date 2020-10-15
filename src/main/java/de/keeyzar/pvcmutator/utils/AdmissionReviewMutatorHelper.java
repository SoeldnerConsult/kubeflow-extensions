package de.keeyzar.pvcmutator.utils;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.AdmissionReviewBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static javax.json.bind.JsonbConfig.FORMATTING;

@ApplicationScoped
public class AdmissionReviewMutatorHelper {
    private static final Logger log = LoggerFactory.getLogger(AdmissionReviewMutatorHelper.class);

    private final JsonPatchCreator jsonPatchCreator;

    private boolean debugAdmissionReview = true;

    @Inject
    public AdmissionReviewMutatorHelper(JsonPatchCreator jsonPatchCreator) {
        this.jsonPatchCreator = jsonPatchCreator;
    }

    //creates an admission review
    public <T> AdmissionReview createAdmissionReview(AdmissionReview review, Class<T> clz, List<ReviewHook<T>> reviewHooks){
        String CREATE = "CREATE";

        AdmissionRequest request = review.getRequest();
        if(CREATE.equals(request.getOperation()) && debugAdmissionReview){
            Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().setProperty(FORMATTING, true));
            log.info("received create admission review for class {}: \n{}\n", clz, jsonb.toJson(review));
            //todo make some great logging
        }

        AdmissionResponseBuilder responseBuilder = new AdmissionResponseBuilder()
                .withAllowed(true)
                .withUid(request.getUid());

        KubernetesResource requestObject = request.getObject();

        if (CREATE.equals(request.getOperation()) && requestObject.getClass().isAssignableFrom(clz)) {
            T parsedObject = (T) requestObject;

            for (ReviewHook<T> revHooks : reviewHooks) {
                Predicate<T> shouldModify = revHooks.getShouldModify();
                Optional<Consumer<T>> preModifyHookOptional = revHooks.getPreModifyHook();
                Consumer<T> modifier = revHooks.getModifier();

                if (shouldModify.test(parsedObject)) {
                    log.info("found consumer interested in mutating pod");
                    preModifyHookOptional.ifPresent((preModifyHook) -> preModifyHook.accept(parsedObject));
                    String encoded = jsonPatchCreator.base64JSONPatchCreator(modifier, parsedObject);

                    responseBuilder
                            .withPatchType("JSONPatch")
                            .withPatch(encoded);
                    log.info("Skipping asking next consumers; as we already handled the pod");
                    break;
                }
            }
        }

        //fix necessary, because we can't fix admissionReview of AdmissionReviewBuilder
        //and we can't set v1beta1 in kubernetes
        AdmissionReview admissionReview = new AdmissionReviewBuilder().withResponse(responseBuilder.build()).build();
        admissionReview.setApiVersion("admission.k8s.io/v1");
        return admissionReview;
    }

    public <T> AdmissionReview createAdmissionReview(AdmissionReview review, Class<T> clz, Predicate<T> shouldModify,
                                                     Consumer<T> modifier) {

        List<ReviewHook<T>> reviewHooks = List.of(new ReviewHook<T>(shouldModify, modifier, Optional.empty()));
        return createAdmissionReview(review, clz, reviewHooks);
    }


    /**
     * This class is intended for registering multiple pod listeners for different types of Pods on a single
     * endpoint, so we do not waste any time
     * --
     * we could simply register multiple Mutating Webhooks, but this is unnecessarily increasing time.
     * @param <T>
     */
    public static class ReviewHook<T> {
        private final Predicate<T> shouldModify;
        private final Consumer<T> modifier;
        private final Optional<Consumer<T>> preModifyHook;

        public ReviewHook(Predicate<T> shouldModify, Consumer<T> modifier, Optional<Consumer<T>> preModifyHook) {
            this.shouldModify = shouldModify;
            this.modifier = modifier;
            this.preModifyHook = preModifyHook;
        }

        public Predicate<T> getShouldModify() {
            return shouldModify;
        }

        public Consumer<T> getModifier() {
            return modifier;
        }

        public Optional<Consumer<T>> getPreModifyHook() {
            return preModifyHook;
        }
    }

    /**
     * disable/enable debugging of ReviewItems
     */
    public void enableDebuggingOfReviewitem(boolean enableDebugging){
        this.debugAdmissionReview = enableDebugging;
    }
}
