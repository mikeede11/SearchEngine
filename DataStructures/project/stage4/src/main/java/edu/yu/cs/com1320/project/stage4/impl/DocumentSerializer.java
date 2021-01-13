package edu.yu.cs.com1320.project.stage4.impl;

import com.google.gson.*;
import edu.yu.cs.com1320.project.stage4.Document;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;

public class DocumentSerializer implements JsonSerializer<DocumentImpl> {

    @Override
    public JsonElement serialize(DocumentImpl d, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        String docText = d.getDocumentAsTxt();
        URI docUri = d.getKey();
        int docHashCode = d.getDocumentTextHashCode();
        Map<String, Integer> wordMap = d.getWordMap();
        object.addProperty("text", docText);
        object.add("key", new Gson().toJsonTree(docUri));
        object.addProperty("txtHash", docHashCode);
        object.add("wordMap", new Gson().toJsonTree(wordMap));
        return object;
    }

}