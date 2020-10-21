package de.keeyzar.tenancyfixer.pods;

import de.keeyzar.tenancyfixer.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * well well well.. definitely missing some tests :)
 */
@ExtendWith(MockitoExtension.class)
class KatibPodModifierTest {
    KatibPodModifier katibPodModifier = new KatibPodModifier();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Pod pod;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Container mainContainer;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Container katibContainer;

    @Test
    public void testThatCommandsAreOnlyDeletedWhenEmpty(){
        setupKatibContainer();
        when(katibContainer.getCommand().isEmpty()).thenReturn(true);

        katibPodModifier.removeCommandEmptyListIfPresent(pod);
        verify(katibContainer, times(1)).setCommand(null);
    }

    @Test
    public void testCommandIsNotDeletedIfNotEmpty(){
        setupKatibContainer();
        when(katibContainer.getCommand().isEmpty()).thenReturn(false);

        katibPodModifier.removeCommandEmptyListIfPresent(pod);
        verify(mainContainer, times(0)).setCommand(null);
    }


    @Test
    void testThatNecessityOfModificationIsCorrectlyChecked() {
        when(pod.getMetadata()
                .getLabels()
                .containsKey(KFEConstants.KF_EXTENSION_LABEL)
        ).thenReturn(true);

        assertTrue(katibPodModifier.isModificationNecessary(pod), "pod should be modified!");
    }

    @Test
    void testThatNoModificationNecessaryIfNoLabelIsFound() {
        when(pod.getMetadata()
                .getLabels()
                .containsKey(KFEConstants.KF_EXTENSION_LABEL)
        ).thenReturn(false);

        assertFalse(katibPodModifier.isModificationNecessary(pod), "pod should not be modified!");
    }

    private void setupKatibContainer(){
        when(pod.getSpec().getContainers()).thenReturn(List.of(katibContainer));
        when(katibContainer.getName()).thenReturn(KFEConstants.KATIB_CONTAINER_NAME);
    }

    @Test
    void expectNoSubstitution() {
        String actual = new KatibPodModifier().replaceFailingFunction("teststring");
        Assertions.assertEquals("teststring", actual);
    }

    @Test
    void expectSubstitution() throws IOException {
        String content;

        try(InputStream stream = getClass().getClassLoader().getResourceAsStream("kale-drop-in-replacement.py")){
            content = new String(stream.readAllBytes());
            content = content.replaceAll("\\R", "\n");
            //any random char sequence, making sure at least any python code is in there
            assert ! content.isBlank();
        }

        KatibPodModifier katibPodModifier = new KatibPodModifier();
        String actual = katibPodModifier.replaceFailingFunction("from kale.common.kfputils                import create_and_wait_kfp_run;");
        Assertions.assertEquals(content + "from kale.common.kfputils                import create_and_wait_kfp_run;", actual);
    }
}