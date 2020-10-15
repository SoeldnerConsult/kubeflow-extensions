package de.keeyzar.pvcmutator.pojo.istio.servicerolebinding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder(setterPrefix = "with", builderClassName = "SubjectIstioBuilder")
@JsonDeserialize(builder = SubjectIstio.SubjectIstioBuilder.class)
@JsonPropertyOrder({"name", "properties"})
public class SubjectIstio {
    @JsonProperty("name")
    public String name;

    @JsonProperty("properties")
    public Map<String, String> properties = new HashMap<>();
}
