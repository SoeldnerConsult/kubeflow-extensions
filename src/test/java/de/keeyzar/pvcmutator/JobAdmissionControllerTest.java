package de.keeyzar.pvcmutator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class JobAdmissionControllerTest {

//    @InjectMock
//    SharedLists sharedLists;

    @Test
    void validateNoMutationWhenReadWriteOnce() throws IOException {
        HashSet<String> value = new HashSet<>();
//        value.add("dog-breed-with-webhook-katib-v4-fbcuv-65jk8jqj");
//        Mockito.when(sharedLists.getTrialList()).thenReturn(value);
//        InputStream jsonFromFileAsInputStream = getClass().getClassLoader().getResourceAsStream("example-job-admission-review.json.json");
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("example-job-admission-review.json");
        AdmissionReview admissionReview = objectMapper.readValue(resourceAsStream, AdmissionReview.class);
//        AdmissionReview admissionReview = new AdmissionReviewMessageBodyReader().readFrom(null, null, null, null, null, jsonFromFileAsInputStream);
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        final String expectedResponse = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1\",\"kind\":\"AdmissionReview\",\"response\":{\"additionalProperties\":{},\"allowed\":true,\"patch\":\"W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL3RlbXBsYXRlL21ldGFkYXRhL2Fubm90YXRpb25zL3NpZGVjYXIuaXN0aW8uaW8vaW5qZWN0IiwidmFsdWUiOiJ0cnVlIn0seyJvcCI6ImFkZCIsInBhdGgiOiIvc3BlYy90ZW1wbGF0ZS9tZXRhZGF0YS9sYWJlbHMvbm90ZWJvb2stbmFtZSIsInZhbHVlIjoidGVzdC1rYWxlIn0seyJvcCI6InJlcGxhY2UiLCJwYXRoIjoiL3NwZWMvdGVtcGxhdGUvc3BlYy9zZXJ2aWNlQWNjb3VudE5hbWUiLCJ2YWx1ZSI6ImRlZmF1bHQtZWRpdG9yIn1d\",\"patchType\":\"JSONPatch\",\"uid\":\"6b649b17-9fdc-4e6c-89ff-0872a76a0fce\"}}";
        String response = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(jsonb.toJson(admissionReview))

                .when().post("/job/mutate")

                .then()
                .statusCode(200)
                .extract().asString();

        MatcherAssert.assertThat(response, is(expectedResponse));
    }
}