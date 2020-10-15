package de.keeyzar.pvcmutator.jobs;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
class JobAdmissionControllerTest {

    @Test
    void integratedAdmissionReviewTest() throws IOException {
        InputStream whichFileToSendAsBody =
                getClass().getClassLoader().getResourceAsStream("example-job-admission-review.json");

        String expectedPatch = encode("[{\"op\":\"add\",\"path\":\"/spec/template/metadata/labels/kubeflow-extension\",\"value\":\"true\"}]");
        final String expectedResponse = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1\",\"kind\":\"AdmissionReview\",\"response\":{\"additionalProperties\":{},\"allowed\":true,\"patch\":\"" +
                                        expectedPatch +
                                        "\",\"patchType\":\"JSONPatch\",\"uid\":\"6b649b17-9fdc-4e6c-89ff-0872a76a0fce\"}}";



        String response = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(whichFileToSendAsBody)

                .when().post("/job/mutate")

                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(response, is(expectedResponse));
    }

    public String encode(String textToEncode){
        return Base64.getEncoder().encodeToString(textToEncode.getBytes());
    }
}