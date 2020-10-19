package de.keeyzar.tenancyfixer.jobs;

import de.keeyzar.tenancyfixer.utils.SharedTrialNameList;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.vertx.core.impl.ConcurrentHashSet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class JobAdmissionControllerTest {
    @InjectMock
    SharedTrialNameList sharedTrialNameList;

    @Test
    void integratedAdmissionReviewTest() throws IOException {
        InputStream whichFileToSendAsBody =
                getClass().getClassLoader().getResourceAsStream("example-job-admission-review.json");

        String expectedPatch = encode("[{\"op\":\"add\",\"path\":\"/spec/template/metadata/annotations/sidecar.istio.io~1inject\",\"value\":\"true\"},{\"op\":\"add\",\"path\":\"/spec/template/metadata/labels/kubeflow-extension\",\"value\":\"true\"},{\"op\":\"replace\",\"path\":\"/spec/template/spec/serviceAccountName\",\"value\":\"default-editor\"}]");
        final String expectedResponse = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1\",\"kind\":\"AdmissionReview\",\"response\":{\"additionalProperties\":{},\"allowed\":true,\"patch\":\"" +
                                        expectedPatch +
                                        "\",\"patchType\":\"JSONPatch\",\"uid\":\"6b649b17-9fdc-4e6c-89ff-0872a76a0fce\"}}";

        ConcurrentHashSet<String> setMock = Mockito.mock(ConcurrentHashSet.class);
        Mockito.when(sharedTrialNameList.getTrialNames()).thenReturn(setMock);
        Mockito.when(setMock.remove(any())).thenReturn(true);

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