package de.keeyzar.pvcmutator.pods;

import de.keeyzar.pvcmutator.utils.AdmissionReviewMutatorHelper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.admission.AdmissionReview;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.vertx.core.cli.Argument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.internal.matchers.Any;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
class PodAdmissionControllerTest {

    @InjectMock
    private NotebookPodModifier notebookPodModifier;
    @InjectMock
    private KatibPodModifier katibPodModifier;
    @InjectMock
    private AdmissionReviewMutatorHelper admissionReviewMutatorHelper;

    @Inject
    private PodAdmissionController podAdmissionController;

    @Test
    void testThatCorrectAmountOfReviewHooksAreAdded() {
        AdmissionReview mockReview = Mockito.mock(AdmissionReview.class);

        ArgumentCaptor<List<AdmissionReviewMutatorHelper.ReviewHook<Pod>>> listArgumentCaptor
                = ArgumentCaptor.forClass(List.class);

        podAdmissionController.mutate(mockReview);

        Mockito.verify(admissionReviewMutatorHelper, times(1))
                .createAdmissionReview(eq(mockReview), eq(Pod.class), listArgumentCaptor.capture());
        Assertions.assertEquals(2, listArgumentCaptor.getValue().size());
    }

    @Test
    void testThatNotebookPodReviewHooksArePutTogether() {
        AdmissionReview mockReview = Mockito.mock(AdmissionReview.class);

        ArgumentCaptor<List<AdmissionReviewMutatorHelper.ReviewHook<Pod>>> listArgumentCaptor
                = ArgumentCaptor.forClass(List.class);

        podAdmissionController.mutate(mockReview);
        Mockito.verify(admissionReviewMutatorHelper, times(1))
                .createAdmissionReview(eq(mockReview), eq(Pod.class), listArgumentCaptor.capture());

        List<AdmissionReviewMutatorHelper.ReviewHook<Pod>> reviewHooks = listArgumentCaptor.getValue();
        AdmissionReviewMutatorHelper.ReviewHook<Pod> podReviewHook = reviewHooks.get(0);

        podReviewHook.getShouldModify().test(any());
        podReviewHook.getModifier().accept(any());

        assertEquals(Optional.empty(), podReviewHook.getPreModifyHook(), "Notebook Pod does not have any pre modify hooks");
        verify(notebookPodModifier, times(1)).isModificationNecessary(any());
        verify(notebookPodModifier, times(1)).modifyNotebookPod(any());
    }

    @Test
    void testThatKatibPodReviewHooksArePutTogether() {
        AdmissionReview mockReview = Mockito.mock(AdmissionReview.class);

        ArgumentCaptor<List<AdmissionReviewMutatorHelper.ReviewHook<Pod>>> listArgumentCaptor
                = ArgumentCaptor.forClass(List.class);

        podAdmissionController.mutate(mockReview);
        Mockito.verify(admissionReviewMutatorHelper, times(1))
                .createAdmissionReview(eq(mockReview), eq(Pod.class), listArgumentCaptor.capture());

        List<AdmissionReviewMutatorHelper.ReviewHook<Pod>> reviewHooks = listArgumentCaptor.getValue();
        AdmissionReviewMutatorHelper.ReviewHook<Pod> podReviewHook = reviewHooks.get(1);

        podReviewHook.getShouldModify().test(any());
        podReviewHook.getModifier().accept(any());
        podReviewHook.getPreModifyHook().ifPresent(e -> e.accept(any()));

        verify(katibPodModifier, times(1)).isModificationNecessary(any());
        verify(katibPodModifier, times(1)).mutatePod(any());
        verify(katibPodModifier, times(1)).removeCommandEmptyListIfPresent(any());
    }


}