package de.keeyzar.pvcmutator.pojo.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
//sometimes its properly camelCased, sometimes not..
//we actually don't care for it, so just ignore it
@JsonIgnoreProperties({"resourceQuotaSpec", "resourcequotaspec", "plugins"})
public class ProfileSpec implements KubernetesResource {

    @JsonProperty
    private Owner owner;

    public ProfileSpec() {
    }

    public ProfileSpec(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ProfileSpec{" +
                "owner=" + owner +
                '}';
    }
}
