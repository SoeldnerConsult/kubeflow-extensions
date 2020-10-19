//package de.keeyzar.pvcmutator.pojo.istio.servicerolebinding;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonPropertyOrder;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import io.fabric8.kubernetes.api.model.KubernetesResource;
//import lombok.Builder;
//import lombok.Data;
//
//@Data
//@Builder(setterPrefix = "with", builderClassName = "RoleRefIstioBuilder")
//@JsonDeserialize(builder = RoleRefIstio.RoleRefIstioBuilder.class)
//@JsonPropertyOrder({"kind", "name"})
//public class RoleRefIstio implements KubernetesResource {
//    @JsonProperty("kind")
//    public String kind;
//
//    @JsonProperty("name")
//    public String name;
//}
