package de.keeyzar.pvcmutator.pods;

import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotebookPodModifierTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Pod podDeep;

    @Mock
    Pod pod;

    NotebookPodModifier notebookPodModifier = new NotebookPodModifier();

    @Test
    public void testThatModificationIsNecessaryWithNotebookLabel(){
        when(podDeep.getMetadata().getLabels().containsKey(KFEConstants.NOTEBOOK_LABEL))
                .thenReturn(true);

        boolean shouldModifyActual = notebookPodModifier.isModificationNecessary(podDeep);

        Assertions.assertTrue(shouldModifyActual, "mutation should be necessary, but isn't");
    }

    @Test
    public void testThatModificationIsNotNecessaryWithoutNotebookLabel(){
        when(podDeep.getMetadata()
                .getLabels()
                .containsKey(KFEConstants.NOTEBOOK_LABEL))
            .thenReturn(false);

        boolean shouldModifyActual = notebookPodModifier.isModificationNecessary(podDeep);

        Assertions.assertFalse(shouldModifyActual, "mutation should NOT be necessary, but was marked as those!");
    }

    @Test
    public void testThatCorrectLabelIsAppended(){
        HashMap<String, String> mockMap = mock(HashMap.class);
        ObjectMeta mockObjectMeta = mock(ObjectMeta.class);
        when(pod.getMetadata()).thenReturn(mockObjectMeta);

        when(mockObjectMeta.getLabels()).thenReturn(mockMap);

        notebookPodModifier.modifyNotebookPod(pod);
        verify(mockMap,times(1)).put(KFEConstants.KF_EXTENSION_LABEL, "true");
        verifyNoMoreInteractions(mockMap);
    }
}