package de.keeyzar.pvcmutator;

import de.keeyzar.pvcmutator.utils.AdmissionReviewMessageBodyReader;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class MutatingAdmissionControllerTest {

    @Test
    void validateNoMutationWhenReadWriteOnce() throws IOException {
        InputStream jsonFromFileAsInputStream = getClass().getClassLoader().getResourceAsStream("example-admission-review.json");
        AdmissionReview admissionReview = new AdmissionReviewMessageBodyReader().readFrom(null, null, null, null, null, jsonFromFileAsInputStream);
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig());

        final String expectedResponse = "{\"additionalProperties\":{},\"apiVersion\":\"admission.k8s.io/v1\",\"kind\":\"AdmissionReview\",\"response\":{\"additionalProperties\":{},\"allowed\":true,\"patch\":\"W3sib3AiOiJyZXBsYWNlIiwicGF0aCI6Ii9zcGVjL2FkZGl0aW9uYWxQcm9wZXJ0aWVzL3J1blNwZWMiLCJ2YWx1ZSI6ImFwaVZlcnNpb246IGJhdGNoL3YxXG5raW5kOiBKb2Jcbm1ldGFkYXRhOlxuICBuYW1lOiBkb2ctYnJlZWQtd2l0aC13ZWJob29rLWthdGliLXY0LXE4ZHRiLWxydm43bXhyXG4gIG5hbWVzcGFjZTogYWRtaW4tbnNcbnNwZWM6XG4gIGJhY2tvZmZMaW1pdDogMFxuICB0ZW1wbGF0ZTpcbiAgICBtZXRhZGF0YTpcbiAgICAgIGFubm90YXRpb25zOlxuICAgICAgICBzaWRlY2FyLmlzdGlvLmlvL2luamVjdDogXCJ0cnVlXCJcbiAgICAgIGxhYmVsczpcbiAgICAgICAgYWNjZXNzLW1sLXBpcGVsaW5lOiBcInRydWVcIlxuICAgIHNwZWM6XG4gICAgICByZXN0YXJ0UG9saWN5OiBOZXZlclxuICAgICAgc2VydmljZUFjY291bnROYW1lOiBkZWZhdWx0LWVkaXRvclxuICAgICAgY29udGFpbmVyczpcbiAgICAgICAgLSBuYW1lOiBkb2ctYnJlZWQtd2l0aC13ZWJob29rLWthdGliLXY0LXE4ZHRiLWxydm43bXhyXG4gICAgICAgICAgaW1hZ2U6IGdjci5pby9hcnJpa3RvL2thdGliLWtmcC10cmlhbDo4ZjM1OGZmXG4gICAgICAgICAgY29tbWFuZDpcbiAgICAgICAgICAgIC0gcHl0aG9uMyAtdSAtYyBcImZyb20ga2FsZS5jb21tb24ua2ZwdXRpbHMgICAgICAgICAgICAgICAgaW1wb3J0IGNyZWF0ZV9hbmRfd2FpdF9rZnBfcnVuOyAgICAgICAgICAgICAgICBjcmVhdGVfYW5kX3dhaXRfa2ZwX3J1biggICAgICAgICAgICAgICAgICAgIHBpcGVsaW5lX2lkPSdlMWE4OTgzZC04Y2VmLTRhYWItODc3My0xMjhiYmFiODdiZjInLCAgICAgICAgICAgICAgICAgICAgcnVuX25hbWU9J2RvZy1icmVlZC13aXRoLXdlYmhvb2sta2F0aWItdjQtcThkdGItbHJ2bjdteHInLCAgICAgICAgICAgICAgICAgICAgZXhwZXJpbWVudF9uYW1lPSdkb2ctYnJlZWQtd2l0aC13ZWJob29rLWthdGliLXY0LXE4ZHRiJyxcbiAgICAgICAgICAgICAgICAgICAgICAgIG5vZGVzX251bWJlcj0nMjUxJywgICAgICAgICAgICAgICAgKVwiXG4ifV0=\",\"patchType\":\"JSONPatch\",\"uid\":\"96f18e61-12ca-4a49-b59f-3950018d236f\"}}";
        String response = given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(jsonb.toJson(admissionReview))

                .when().post("/mutate")

                .then()
                .statusCode(200)
                .extract().asString();

        MatcherAssert.assertThat(response, is(expectedResponse));
    }
}