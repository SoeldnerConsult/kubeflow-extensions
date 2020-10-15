package de.keeyzar.pvcmutator.trials;

import de.keeyzar.pvcmutator.pojo.Trials.Trial;
import de.keeyzar.pvcmutator.pojo.Trials.Trial.TrialBuilder;
import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

class TrialsMutatorTest {

    @Test
    void makeSureTrialsInKubeflowNamespaceAreNotModified() {
        TrialsMutator trialsMutator = new TrialsMutator();

        Trial trialUnderTest = generateTrial("kubeflow");
        boolean actualValue = trialsMutator.shouldWeModifyCorrespondingJobs(trialUnderTest);

        Assertions.assertFalse(actualValue, "we should not modify the trial, because" +
                "it is in the kubeflow namespace");
    }

    @Test
    void testThatTrialsAreModifiedWhenNotInKubeflowNamespace() {
        TrialsMutator trialsMutator = new TrialsMutator();

        Trial trialUnderTest = generateTrial("any");
        boolean actualValue = trialsMutator.shouldWeModifyCorrespondingJobs(trialUnderTest);

        Assertions.assertTrue(actualValue, "trial should've been modified, it is not");
    }

    @Test
    void testThatLabelIsAddedWhenModificationHappens() {
        TrialsMutator trialsMutator = new TrialsMutator();
        Trial trialUnderTest = generateTrial("any");
        trialUnderTest.getMetadata().setLabels(new HashMap<>());

        trialsMutator.mutateTrial(trialUnderTest);
        Assertions.assertEquals("true", trialUnderTest.getMetadata().getLabels().get(KFEConstants.KF_EXTENSION_LABEL));
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