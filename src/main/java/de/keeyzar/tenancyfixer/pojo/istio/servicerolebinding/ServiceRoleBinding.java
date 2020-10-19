//package de.keeyzar.pvcmutator.pojo.istio.servicerolebinding;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonPropertyOrder;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import io.fabric8.kubernetes.api.model.HasMetadata;
//import io.fabric8.kubernetes.api.model.KubernetesResource;
//import io.fabric8.kubernetes.api.model.ObjectMeta;
//import io.fabric8.kubernetes.api.model.Status;
//import io.fabric8.kubernetes.client.CustomResourceList;
//import lombok.AccessLevel;
//import lombok.Builder;
//import lombok.Data;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Data
//@Builder(setterPrefix = "with", builderClassName = "ServiceRoleBindingBuilder")
//@JsonDeserialize(builder = ServiceRoleBinding.ServiceRoleBindingBuilder.class)
//@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec", "additionalProperties"})
//@JsonIgnoreProperties("status")
//public class ServiceRoleBinding implements HasMetadata {
//
//    @JsonProperty("apiVersion")
//    private String apiVersion = "rbac.istio.io/v1alpha1";
//    @JsonProperty("kind")
//    private String kind = "ServiceRoleBinding";
//    @JsonProperty("metadata")
//    private ObjectMeta metadata;
//    @JsonProperty("spec")
//    private ServiceRoleBindingSpec spec;
//    @JsonIgnore
//    private Status status;
//    @JsonProperty("additionalProperties")
//    private Map<String, Object> additionalProperties = new HashMap();
//}