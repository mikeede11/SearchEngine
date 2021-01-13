package edu.yu.cs.com1320.project.stage4.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class DocumentDeserializer implements JsonDeserializer<DocumentImpl> {
    @Override
    public DocumentImpl deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String docText = jsonElement.getAsJsonObject().get("text").toString();
        int hashText = jsonElement.getAsJsonObject().get("txtHash").getAsInt();
        URI key = null;
        try { key = new URI(jsonElement.getAsJsonObject().get("key").toString());
        } catch (URISyntaxException e) { e.printStackTrace(); }
        Gson gson = new Gson();
        Type typeOfMap = new TypeToken<HashMap<String, Integer>>(){}.getType();
        String mapInfo = jsonElement.getAsJsonObject().get("wordMap").toString();
        HashMap<String, Integer> wordMap = gson.fromJson(mapInfo,typeOfMap);
        DocumentImpl deserializedDoc = new DocumentImpl(key,docText,hashText);
        deserializedDoc.setWordMap(wordMap);
        return  deserializedDoc;



    }
}
