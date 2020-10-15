package de.keeyzar.pvcmutator.pojo.Trials;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class TrialTest {

    @Test
    public void testDeserializing() throws IOException {
        Trial originalTrial = new Trial.TrialBuilder()
                .withApiVersion("kubeflow.org/v1alpha3")
                .withKind("Trial").build();

        ObjectMapper objectMapper = new ObjectMapper();
        String objectSerialized = objectMapper.writeValueAsString(originalTrial);
        MatcherAssert.assertThat(objectSerialized, CoreMatchers.is("{\"apiVersion\":\"kubeflow.org/v1alpha3\",\"kind\":\"Trial\",\"metadata\":null,\"additionalProperties\":null}"));

        Trial deserializedTrial = objectMapper.readValue(objectSerialized, Trial.class);
        Assertions.assertEquals(deserializedTrial, originalTrial);
    }
}