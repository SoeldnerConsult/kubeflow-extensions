package de.keeyzar.pvcmutator.trials;

import de.keeyzar.pvcmutator.pojo.trials.Trial;
import de.keeyzar.pvcmutator.utils.KFEConstants;
import de.keeyzar.pvcmutator.utils.SharedTrialNameList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.vertx.core.impl.ConcurrentHashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialsMutatorTest {

    @Mock
    SharedTrialNameList sharedTrialNameList;

    @Test
    void makeSureTrialsInKubeflowNamespaceAreNotModified() {
        TrialsMutator trialsMutator = new TrialsMutator(sharedTrialNameList);

        Trial trialUnderTest = generateTrial("kubeflow");
        boolean actualValue = trialsMutator.shouldWeModifyCorrespondingJobs(trialUnderTest);

        Assertions.assertFalse(actualValue, "we should not modify the trial, because" +
                "it is in the kubeflow namespace");
    }

    @Test
    void testThatTrialsAreModifiedWhenNotInKubeflowNamespace() {
        TrialsMutator trialsMutator = new TrialsMutator(sharedTrialNameList);

        Trial trialUnderTest = generateTrial("any");
        boolean actualValue = trialsMutator.shouldWeModifyCorrespondingJobs(trialUnderTest);

        Assertions.assertTrue(actualValue, "trial should've been modified, it is not");
    }

    @Test
    void testThatLabelIsAddedWhenModificationHappens() {
        TrialsMutator trialsMutator = new TrialsMutator(sharedTrialNameList);
        Trial trialUnderTest = generateTrial("any");
        trialUnderTest.getMetadata().setLabels(new HashMap<>());

        trialsMutator.mutateTrial(trialUnderTest);
        Assertions.assertEquals("true", trialUnderTest.getMetadata().getLabels().get(KFEConstants.KF_EXTENSION_LABEL));
    }


    @Test
    void testThatJobNameIsAddedIntoListWhenModificationHappens() {
        ConcurrentHashSet<String> mock = mock(ConcurrentHashSet.class);
        when(sharedTrialNameList.getTrialNames()).thenReturn(mock);

        TrialsMutator trialsMutator = new TrialsMutator(sharedTrialNameList);

        Trial trialUnderTest = generateTrial("any");
        trialUnderTest.getMetadata().setLabels(new HashMap<>());
        trialUnderTest.getMetadata().setName("coolName");

        trialsMutator.mutateTrial(trialUnderTest);
        verify(mock, times(1))
                .add("coolName");
    }

    private Trial generateTrial(String namespace) {
        ObjectMeta kubeflowNamespace = new ObjectMetaBuilder()
                .withNamespace(namespace)
                .build();
        Trial trialUnderTest = Trial.builder()
                .withMetadata(kubeflowNamespace)
                .build();
        return trialUnderTest;
    }
}