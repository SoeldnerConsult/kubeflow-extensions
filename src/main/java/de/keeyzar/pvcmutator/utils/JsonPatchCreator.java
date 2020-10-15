package de.keeyzar.pvcmutator.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import java.io.StringReader;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * helper class for json diff creation
 */
@ApplicationScoped
public class JsonPatchCreator {
    private static final Logger log = LoggerFactory.getLogger(JsonPatchCreator.class);

    public <T> String base64JSONPatchCreator(Consumer<T> modifier, T object){
        if (modifier == null){
            log.error("This method is not intended to be called without a corresponding" +
                    " modifier!");
            //todo throw error
        }

        log.info("Creating patch for {}", object.getClass());

        JsonObject original = toJsonObject(object);
        modifier.accept(object);
        JsonObject mutated = toJsonObject(object);

        String patch = Json.createDiff(original, mutated).toString();
        String encoded = Base64.getEncoder().encodeToString(patch.getBytes());

        log.info("patching with patch as json: {}", patch);
        log.info("patch base64 encoded: {}", encoded);

        return encoded;
    }

    private JsonObject toJsonObject(Object object) {
        return Json.createReader(new StringReader(JsonbBuilder.create().toJson(object))).readObject();
    }
}
