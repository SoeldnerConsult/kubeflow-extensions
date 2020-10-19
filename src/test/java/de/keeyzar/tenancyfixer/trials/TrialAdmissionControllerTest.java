package de.keeyzar.tenancyfixer.trials;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;

@QuarkusTest
class TrialAdmissionControllerTest {

    @Test
    void integratedAdmissionReviewMutationTest() throws IOException {
        InputStream whichFileToSendAsBody =
                getClass().getClassLoader().getResourceAsStream("example-trial-admission-review.json");


        //this is the expected jsonpatch, we encode it to base64
        String encodedPatch = encode("[{\"op\":\"add\",\"path\":\"/metadata/labels/kubeflow-extension\",\"value\":\"true\"}]");
        //this is the aggregated result
        final String expectedResponse = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1\",\"kind\":\"AdmissionReview\",\"response\":{\"additionalProperties\":{},\"allowed\":true,\"patch\":\"" +
                                         encodedPatch +
                                        "\",\"patchType\":\"JSONPatch\",\"uid\":\"96f18e61-12ca-4a49-b59f-3950018d236f\"}}";

        String response = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(whichFileToSendAsBody)

                .when().post("/trial/mutate")

                .then()
                .statusCode(200)
                .extract().asString();

        assertThat(response, is(expectedResponse));
    }
    private String encode(String textToEncode){
        return  Base64.getEncoder().encodeToString(textToEncode.getBytes());
    }

}