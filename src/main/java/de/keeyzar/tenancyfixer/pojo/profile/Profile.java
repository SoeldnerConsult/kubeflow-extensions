package de.keeyzar.tenancyfixer.pojo.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;

public class Profile extends CustomResource {

    @JsonProperty("apiVersion")
    private String apiVersion = "kubeflow.org/v1";

    @JsonProperty("kind")
    private String kind = "Profile";

    @JsonProperty()
    private ObjectMeta metadata;

    @JsonProperty()
    private ProfileSpec spec;

    @JsonProperty()
    private ProfileStatus status;

    public Profile() {
    }

    public Profile(String apiVersion, String kind, ObjectMeta metadata, ProfileSpec spec, ProfileStatus status) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.metadata = metadata;
        this.spec = spec;
        this.status = status;
    }

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String version) {
        this.apiVersion = version;
    }

    @Override
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public ProfileSpec getSpec() {
        return spec;
    }

    public void setSpec(ProfileSpec spec) {
        this.spec = spec;
    }

    public ProfileStatus getStatus() {
        return status;
    }

    public void setStatus(ProfileStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "apiVersion='" + apiVersion + '\'' +
                ", kind='" + kind + '\'' +
                ", metadata=" + metadata +
                ", spec=" + spec +
                ", status=" + status +
                '}';
    }
}
