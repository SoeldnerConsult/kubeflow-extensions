package de.keeyzar.pvcmutator.pojo.istio.servicerolebinding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder(setterPrefix = "with", builderClassName = "ServiceRoleBindingSpecBuilder")
@JsonDeserialize(builder = ServiceRoleBindingSpec.ServiceRoleBindingSpecBuilder.class)
@JsonPropertyOrder({"roleRef", "subjects", "additionalProperties"})
public class ServiceRoleBindingSpec implements KubernetesResource {

    @JsonProperty("roleRef")
    public RoleRefIstio roleRef;

    @JsonProperty("subjects")
    public SubjectIstio subject;

    @JsonProperty("additionalProperties")
    private Map<String, Object> additionalProperties = new HashMap();
}
