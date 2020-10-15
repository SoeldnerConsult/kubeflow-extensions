package de.keeyzar.pvcmutator.jobs;

import de.keeyzar.pvcmutator.utils.KFEConstants;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobMutatorTest {
    @Test
    public void jobNeedModificationWithKubeflowExtensionLabel(){
        JobMutator jobMutator = new JobMutator();
        Job job = new JobBuilder().withNewMetadata()
                .withLabels(Map.of(KFEConstants.KF_EXTENSION_LABEL, "true"))
                .endMetadata()
                .build();

        boolean actualValue = jobMutator.doesJobNeedModification(job);
        assertTrue(actualValue, "Job should need a modification!");
    }

    @Test
    public void kubeflowExtensionLabelValueIsImportant(){
        JobMutator jobMutator = new JobMutator();
        Job job = new JobBuilder().withNewMetadata()
                .withLabels(Map.of(KFEConstants.KF_EXTENSION_LABEL, "false"))
                .endMetadata()
                .build();

        boolean actualValue = jobMutator.doesJobNeedModification(job);
        assertFalse(actualValue, "Job should need a modification!");
    }

    @Test
    public void kubeflowExtensionLabelValueIsImportant_2(){
        JobMutator jobMutator = new JobMutator();
        Job job = new JobBuilder().withNewMetadata()
                .withLabels(Map.of(KFEConstants.KF_EXTENSION_LABEL, ""))
                .endMetadata()
                .build();

        boolean actualValue = jobMutator.doesJobNeedModification(job);
        assertFalse(actualValue, "Job should need a modification!");
    }

    @Test
    void templateJobGetsMutatedCorrectly() {
        JobMutator jobMutator = new JobMutator();
        Job job = new JobBuilder()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(Map.of())
                .endMetadata()
                .endTemplate()
                .endSpec().build();

        jobMutator.mutateJob(job);

        assertEquals("true", job.getSpec().getTemplate().getMetadata().getLabels().get(KFEConstants.KF_EXTENSION_LABEL));
    }

}