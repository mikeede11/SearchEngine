package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File file;

    public DocumentPersistenceManager(File baseDir) {
        this.file = baseDir;
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {//might need to implement
        String serializedDoc = serializeDoc(val);
        String filePath = processFilePath(uri);//takes out the protocol and switches any "\" to File.separator
        String dirStruc = filePath.substring(0, filePath.lastIndexOf(File.separator));
        String fileName = processJsonFileName(filePath);
        //create the file and directory structure
        File myFile = new File(dirStruc);
        if (!myFile.isDirectory()) {
            myFile.mkdirs();
        }
        File myNewFile = new File(myFile + fileName);
        myNewFile.createNewFile();
        //write to file
        try {
            OutputStream os = new FileOutputStream(myNewFile);
            PrintWriter pw = new PrintWriter(os);
            pw.print(serializedDoc);
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        String filePath = processFilePath(uri);
        String fileName = processJsonFileName(filePath);
        filePath = filePath.substring(0, filePath.lastIndexOf(File.separator));
        String absfilePath = filePath + fileName;
        File file = new File(absfilePath);
        if (file.exists()) {
            String json = new String(Files.readAllBytes(Paths.get(absfilePath)));
            Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer()).create();
            Files.delete(Paths.get(absfilePath));//TODO STILL NEED TO FIGURE OUT HOW TO DELETE FOLDERS
            DocumentImpl deCerealDoc = gson.fromJson(json, DocumentImpl.class);
            return deCerealDoc;
        } else {
            return null;
        }
    }

    private String processFilePath(URI uri) {
        String filePath = uri.getSchemeSpecificPart();//uri.getAuthority() + uri.getPath()
        filePath = filePath.replace("/", File.separator).replace("\\", File.separator);
        filePath = this.file + File.separator + filePath;
        return filePath;
    }

    private String processJsonFileName(String filePath) {
        int index = filePath.lastIndexOf(File.separator);
        return filePath.substring(index) + ".json";//this will be the filename;
    }

    private String serializeDoc(Document val) {
        //DocumentSerializer ds = new DocumentSerializer();
        Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentSerializer()).setPrettyPrinting().create();
        String string = gson.toJson(val);
        return string;
    }

    protected class DocumentSerializer implements JsonSerializer<Document> {
        @Override
        public JsonElement serialize(Document d, Type type, JsonSerializationContext jsonSerializationContext) {
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

    protected class DocumentDeserializer implements JsonDeserializer<Document> {
        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String docText = jsonElement.getAsJsonObject().get("text").getAsString();
            int hashText = jsonElement.getAsJsonObject().get("txtHash").getAsInt();
            URI key = null;
            try {
                key = new URI(jsonElement.getAsJsonObject().get("key").getAsString());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            Gson gson = new Gson();
            Type typeOfMap = new TypeToken<HashMap<String, Integer>>() {
            }.getType();
            String mapInfo = jsonElement.getAsJsonObject().get("wordMap").toString();
            HashMap<String, Integer> wordMap = gson.fromJson(mapInfo, typeOfMap);
            Document deserializedDoc = new DocumentImpl(key, docText, hashText);
            deserializedDoc.setWordMap(wordMap);
            return deserializedDoc;
        }
    }
}
