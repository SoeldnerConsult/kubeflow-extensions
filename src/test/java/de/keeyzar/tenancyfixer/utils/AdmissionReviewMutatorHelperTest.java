package de.keeyzar.tenancyfixer.utils;

import de.keeyzar.tenancyfixer.utils.AdmissionReviewMutatorHelper.ReviewHook;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionReviewMutatorHelperTest {

    @Mock
    JsonPatchCreator mockPatchCreator;
    @Mock
    AdmissionReview admissionReview;
    @Mock
    AdmissionRequest admissionRequest;

    @Mock
    Consumer<TestClass> mockModifier;
    @Mock
    Consumer<TestClass> mockPreModifier;
    @Mock
    Predicate<TestClass> mockPrechecker;

    @InjectMocks
    AdmissionReviewMutatorHelper admissionReviewMutatorHelper;

    @BeforeEach
    public void disableDebugging(){
        //otherwise a mock will be marshalled resulting in errors. :)
        admissionReviewMutatorHelper.enableDebuggingOfReviewitem(false);
    }


    @Test
    void testThatAdmissionReviewCallsHooksInCorrectOrder() {
        TestClass passedTestClass = prepareAdmissionReview();


        List<Integer> integerList = new ArrayList<>();
        Predicate<TestClass> testPredicate = (testClass) -> {
            integerList.add(1);
            return true;
        };
        Consumer<TestClass> testPreHook = (testClass) -> {
            integerList.add(2);
        };

        Consumer<TestClass> testModifier = (testClass) -> {};
        //assume this will call our modifier (after mutation; as it should)
        when(mockPatchCreator.base64JSONPatchCreator(any(), any())).thenAnswer((e) -> {
            integerList.add(3);
            return "encodedStringUnencoded";
        });

        admissionReviewMutatorHelper.createAdmissionReview(admissionReview,
                TestClass.class,
                List.of(new ReviewHook<>(testPredicate, testModifier, Optional.of(testPreHook))));

        //1 and 2 only.. no 3, because testModifier is not called in the class to test, rather
        //its passed to jsonpatchcreator
        assertEquals(List.of(1,2,3), integerList);


        verify(mockPatchCreator, times(1)).base64JSONPatchCreator(testModifier, passedTestClass);
    }

    @Test
    void testThatOnlyOneReviewHookIsCalledWhenTrue() {
        prepareAdmissionReview();

        when(mockPrechecker.test(any())).thenReturn(true).thenReturn(false);
        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)),
                        new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));


        admissionReviewMutatorHelper.createAdmissionReview(admissionReview, TestClass.class, reviewHooks);

        verify(mockPrechecker, times(1)).test(any());
        verify(mockPatchCreator, times(1)).base64JSONPatchCreator(any(), any());

    }

    @Test
    void testEachReviewHookIsTested() {
        prepareAdmissionReview();

        when(mockPrechecker.test(any())).thenReturn(false).thenReturn(false).thenReturn(true);
        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)),
                        new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)),
                        new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        admissionReviewMutatorHelper.createAdmissionReview(admissionReview, TestClass.class, reviewHooks);

        verify(mockPrechecker, times(3)).test(any());
        verify(mockPatchCreator, times(1)).base64JSONPatchCreator(any(), any());
    }

    @Test
    void testThatNoModifierIsCalledWhenPreCheckFails() {
        prepareAdmissionReview();

        when(mockPrechecker.test(any())).thenReturn(false).thenReturn(false).thenReturn(false);
        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)),
                        new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)),
                        new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        admissionReviewMutatorHelper.createAdmissionReview(admissionReview, TestClass.class, reviewHooks);

        verify(mockPrechecker, times(3)).test(any());
        verify(mockPatchCreator, times(0)).base64JSONPatchCreator(any(), any());
    }

    @Test
    void testThatIncorrectRequestObjectDoesNotThrowAnError() {
        when(admissionReview.getRequest()).thenReturn(admissionRequest);
        when(admissionRequest.getOperation()).thenReturn("CREATE");
        when(admissionRequest.getObject()).thenReturn(new Pod());

        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        admissionReviewMutatorHelper.createAdmissionReview(admissionReview, TestClass.class, reviewHooks);
        verify(mockPrechecker, times(0)).test(any());
        verify(admissionReview, times(1)).getRequest();
        verify(admissionRequest, times(1)).getObject();
    }

    @Test
    void testThatNotCreateOperationIsIgnored() {
        when(admissionReview.getRequest()).thenReturn(admissionRequest);
        when(admissionRequest.getOperation()).thenReturn("UPDATE");
        when(admissionRequest.getObject()).thenReturn(new Pod());

        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        admissionReviewMutatorHelper.createAdmissionReview(admissionReview, TestClass.class, reviewHooks);
        verify(mockPrechecker, times(0)).test(any());
        verify(admissionReview, times(1)).getRequest();
        verify(admissionRequest, times(1)).getObject();
    }

    @Test
    void testThatAdmissionApiVersionIsFixed() {
        prepareAdmissionReview();
        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        AdmissionReview admissionReview = admissionReviewMutatorHelper.createAdmissionReview(this.admissionReview, TestClass.class, reviewHooks);

        Assertions.assertEquals("admission.k8s.io/v1", admissionReview.getApiVersion(), "apiVersion was not fixed..");
    }

    @Test
    void testThatPatchIsWithinAdmissionReview() {
        prepareAdmissionReview();
        List<ReviewHook<TestClass>> reviewHooks =
                List.of(new ReviewHook<>(mockPrechecker, mockModifier, Optional.of(mockPreModifier)));

        when(mockPrechecker.test(any())).thenReturn(true);
        when(mockPatchCreator.base64JSONPatchCreator(any(), any())).thenReturn("W3thZGR9XQ==");


        AdmissionReview admissionReview = admissionReviewMutatorHelper.createAdmissionReview(this.admissionReview, TestClass.class, reviewHooks);
        AdmissionResponse response = admissionReview.getResponse();
        Assertions.assertEquals("JSONPatch", response.getPatchType(), "patch type is incorrect");
        Assertions.assertEquals("W3thZGR9XQ==", response.getPatch(),
                "patch is not (or not correctly) included in the final review Object");
    }




    private TestClass prepareAdmissionReview() {
        when(admissionReview.getRequest()).thenReturn(admissionRequest);
        when(admissionRequest.getOperation()).thenReturn("CREATE");
        TestClass passedTestClass = new TestClass();
        when(admissionRequest.getObject()).thenReturn(passedTestClass);
        return passedTestClass;
    }


    public static class TestClass implements KubernetesResource {
        String s;
    }
}