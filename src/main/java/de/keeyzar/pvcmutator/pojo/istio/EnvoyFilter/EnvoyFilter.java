//package de.keeyzar.pvcmutator.pojo.istio.EnvoyFilter;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonPropertyOrder;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import io.fabric8.kubernetes.api.model.KubernetesResource;
//import io.fabric8.kubernetes.api.model.ObjectMeta;
//import lombok.Builder;
//import lombok.Data;
//
//import java.util.List;
//
//@Data
//@Builder(setterPrefix = "with", builderClassName = "EnvoyFilterBuilder")
//@JsonDeserialize(builder = EnvoyFilter.EnvoyFilterBuilder.class)
//@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
//@JsonIgnoreProperties({"spec"})
//public class EnvoyFilter implements KubernetesResource {
//    @JsonProperty("apiVersion")
//    private String apiVersion = "networking.istio.io/v1alpha3";
//    @JsonProperty("kind")
//    private String kind = "EnvoyFilter";
//    @JsonProperty("metadata")
//    private ObjectMeta metadata;
//    @JsonIgnore()
//    private KubernetesResource spec;
//}
////
////@JsonDeserialize
////class EVSpec implements KubernetesResource {
////    @JsonProperty("configPatches")
////    List<EVConfigPatches> configPatches;
////    @JsonProperty("workloadSelector")
////    private EVWorkloadSelector workloadSelector;
////}
////
////class EVConfigPatches implements KubernetesResource{
////    @JsonProperty("applyTo")
////    private String applyTo;
////    @JsonProperty("match")
////    private EVMatch match;
////    @JsonProperty("patch")
////    private EVPatch patch;
////}
////
////class EVPatch implements KubernetesResource{
////    @JsonProperty("operation")
////    private String operation;
////    @JsonProperty("value")
////    private EVValue value;
////}
////class EVValue implements KubernetesResource{
////    @JsonProperty("request_headers_to_add")
////    private List<EVRequestHeaders> requestHeaders;
////}
////class EVRequestHeaders implements KubernetesResource {
////    @JsonProperty("append")
////    private String append;
////    @JsonProperty("header")
////    private EVHeader header;
////}
////class EVHeader implements KubernetesResource {
////    @JsonProperty("key")
////    private String key;
////    @JsonProperty("value")
////    private String value;
////}
////
////class EVMatch implements KubernetesResource {
////    @JsonProperty("context")
////    private String context;
////    @JsonProperty("routeConfiguration")
////    private EVRouteConfiguration routeConfiguration;
////}
////
////class EVRouteConfiguration implements KubernetesResource{
////    @JsonProperty("vhost")
////    private EVVhost vhost;
////}
////
////class EVVhost implements KubernetesResource {
////    @JsonProperty("name")
////    private String name;
////    @JsonProperty("route")
////    private EVRoute route;
////}
////
////class EVRoute implements KubernetesResource {
////    @JsonProperty("name")
////    private String name;
////}
////
////class EVWorkloadSelector implements KubernetesResource {
////    @JsonProperty("labels")
////    private EVLabels labels;
////}
////class EVLabels implements KubernetesResource {
////    @JsonProperty("kubeflow-extension")
////    private String kubeflowExtension;
////}