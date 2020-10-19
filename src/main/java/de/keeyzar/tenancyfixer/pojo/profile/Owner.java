package de.keeyzar.tenancyfixer.pojo.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

/**
 * we need to access the owner, so we can create a proper
 * EnvoyFilter
 */
@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class Owner implements KubernetesResource {

    @JsonProperty
    private String name;

    @JsonProperty
    private String kind;

    public Owner() {
    }

    public Owner(String name, String kind) {
        this.name = name;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
