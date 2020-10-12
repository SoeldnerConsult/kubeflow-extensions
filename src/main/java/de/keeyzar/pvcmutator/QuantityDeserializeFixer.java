//package de.keeyzar.pvcmutator;
//
//import io.fabric8.kubernetes.api.model.KubernetesResource;
//import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
//import io.fabric8.kubernetes.api.model.Quantity;
//import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
//import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
//
//import javax.enterprise.context.ApplicationScoped;
//import java.util.Map;
//
///**
// * This class solely exists because multiple serialization/deserialization errors start cropping up,
// * when using tests.. and yes, Tests are necessary;
// */
//@ApplicationScoped
//public class QuantityDeserializeFixer {
//    /**
//     * this is done, because json deserialization for Quantity does not remove the double quotes..
//     */
//    void fixDeserializeError(AdmissionReview admissionReview){
//        AdmissionRequest request = admissionReview.getRequest();
//        KubernetesResource object = request.getObject();
//        PersistentVolumeClaim pvc = (PersistentVolumeClaim) object;
//        Map<String, Quantity> requests = pvc.getSpec().getResources().getRequests();
//        Quantity storage = requests.getOrDefault("storage", new Quantity());
//        String amount = storage.getAmount();
//        String format = storage.getFormat();
//        storage.setAmount(amount.replace("\"", ""));
//        storage.setFormat(format.replace("\"", ""));
//    }
//}
