package de.keeyzar.pvcmutator.utils;

import io.netty.handler.codec.base64.Base64Encoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

class JsonPatchCreatorTest {

    @Test
    void patcherCallsMutator() {
        JsonPatchCreator jsonPatchCreator = new JsonPatchCreator();
        AtomicBoolean gotCalled = new AtomicBoolean(false);
        jsonPatchCreator.base64JSONPatchCreator((e) -> gotCalled.set(true), gotCalled);

        Assertions.assertTrue(gotCalled.get(), "Mutator did not receive a call..");
    }

    @Test
    void mutatorCalledAtCorrectTime(){
        TestClass tc = new TestClass("Hi");
        JsonPatchCreator jsonPatchCreator = new JsonPatchCreator();

        String jsonPatchExpected = "[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Bye\"}]";
        String encodedExpected = Base64.getEncoder().encodeToString(jsonPatchExpected.getBytes());

        String encodedActual = jsonPatchCreator.base64JSONPatchCreator((e) -> e.setName("Bye"), tc);


        Assertions.assertEquals(encodedExpected, encodedActual, "encoding is not working as expected!");
    }

    @Test
    void ifNothingIsDoneNoPatchIsCreated(){
        TestClass tc = new TestClass("Hi");
        JsonPatchCreator jsonPatchCreator = new JsonPatchCreator();
        String encodedStringExpected = Base64.getEncoder().encodeToString("[]".getBytes());
        String encodedStringActual = jsonPatchCreator.base64JSONPatchCreator((e) -> {}, tc);
        Assertions.assertEquals(encodedStringExpected, encodedStringActual);
    }

    public static class TestClass{
        private String name;

        public TestClass(String name) {
            this.name = name;
        }

        public TestClass() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}