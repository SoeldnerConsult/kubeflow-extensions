package de.keeyzar.tenancyfixer.jobs;

import de.keeyzar.tenancyfixer.utils.KFEConstants;
import de.keeyzar.tenancyfixer.utils.SharedTrialNameList;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMutatorTest {
    @Mock
    SharedTrialNameList sharedTrialNameList;
    @Mock
    Set<String> trialNamesSet;


    @Test
    public void whenJobNameFoundInSharedListMutationIsNecessary(){
        JobMutator jobMutator = new JobMutator(sharedTrialNameList);
        Job job = new JobBuilder().withNewMetadata()
                .withName("testName")
                .endMetadata()
                .build();

        sharedTrialNameList.setTrialNames(trialNamesSet);
        when(sharedTrialNameList.getTrialNames()).thenReturn(trialNamesSet);
        when(trialNamesSet.remove("testName")).thenReturn(true);


        boolean actualValue = jobMutator.doesJobNeedModification(job);
        assertTrue(actualValue, "Job should need a modification!");
    }

    @Test
    public void whenJobNameIsNotFoundInSharedListMutationIsNotNecessary(){
        JobMutator jobMutator = new JobMutator(sharedTrialNameList);
        Job job = new JobBuilder().withNewMetadata()
                .endMetadata()
                .build();

        sharedTrialNameList.setTrialNames(trialNamesSet);
        when(sharedTrialNameList.getTrialNames()).thenReturn(trialNamesSet);
        when(trialNamesSet.remove(any())).thenReturn(false);

        boolean actualValue = jobMutator.doesJobNeedModification(job);
        assertFalse(actualValue, "Job should need a modification!");
    }

    @Test
    void templateJobGetsMutatedCorrectly() {
        JobMutator jobMutator = new JobMutator(sharedTrialNameList);
        Job job = new JobBuilder()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(Map.of())
                .withAnnotations(Map.of())
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .endTemplate()
                .endSpec().build();

        jobMutator.mutateJob(job);

        PodTemplateSpec modifiedTemplate = job.getSpec().getTemplate();
        assertEquals("true", modifiedTemplate.getMetadata().getLabels().get(KFEConstants.KF_EXTENSION_LABEL));
        assertEquals("true", modifiedTemplate.getMetadata().getAnnotations().get("sidecar.istio.io~1inject"));
        assertEquals("default-editor", modifiedTemplate.getSpec().getServiceAccountName());
    }

}