//package de.keeyzar.pvcmutator.pojo.istio.servicerolebinding;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonPropertyOrder;
//import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
//import io.fabric8.kubernetes.api.model.KubernetesResource;
//import io.smallrye.mutiny.tuples.Tuple2;
//import lombok.Builder;
//import lombok.Data;
//
//@Data
//@Builder(setterPrefix = "with", builderClassName = "SubjectIstioBuilder")
//@JsonDeserialize(builder = SubjectIstio.SubjectIstioBuilder.class)
//@JsonPropertyOrder({"name", "properties"})
//public class SubjectIstio implements KubernetesResource {
//
//    @JsonProperty("properties")
//    public SubjectIstioProperty properties;
//}
//
//@Data
//@Builder(setterPrefix = "with", builderClassName = "SubjectIstioPropertyBuilder")
//@JsonDeserialize(builder = SubjectIstioProperty.SubjectIstioPropertyBuilder.class)
//class SubjectIstioProperty implements KubernetesResource {
//
//    @JsonProperty("source.principal")
//    public String sourcePrincipal;
//}
//
//
