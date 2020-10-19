package de.keeyzar.tenancyfixer.pojo.trials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Status;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
@Data
@Builder(setterPrefix = "with", builderClassName = "TrialBuilder", access = AccessLevel.PUBLIC)
@JsonDeserialize(builder = Trial.TrialBuilder.class)
@JsonPropertyOrder({"apiVersion", "kind", "metadata", "spec"})
@JsonIgnoreProperties("status")
public class Trial implements HasMetadata {

    @JsonProperty("apiVersion")
    private String apiVersion = "kubeflow.org/v1alpha3";
    @JsonProperty("kind")
    private String kind = "Trial";
    @JsonProperty("metadata")
    private ObjectMeta metadata;
    @JsonIgnore()
    private Object spec;
    @JsonIgnore
    private Status status;
    @JsonProperty("additionalProperties")
    private Map<String, Object> additionalProperties = new HashMap();
}
