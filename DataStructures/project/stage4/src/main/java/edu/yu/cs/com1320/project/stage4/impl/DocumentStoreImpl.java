package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.Stack;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    static enum DocumentFormat {PDF, TXT}

    private HashTableImpl<URI, Document> hTable = new HashTableImpl<URI, Document>();
    StackImpl<Undoable> commandStack = new StackImpl<>();
    private TrieImpl<Document> searchTree = new TrieImpl<>();
    private MinHeap<Document> useTimeMinHeap = new MinHeapImpl();
    private int maxCount;
    private int maxBytes;
    private int currentCount = 0;
    private int currentBytes = 0;
    private boolean maxCountSet;
    private boolean maxBytesSet;

    public int putDocument(InputStream input, URI uri, DocumentStore.DocumentFormat format) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            if (hTable.get(uri) == null) {
                return 0;
            }
            int docHashCode = hTable.get(uri).getDocumentTextHashCode();
            deleteDocument(uri);
            return docHashCode;
        }//b/c if User inputs null we are supposed to delete document. return 0 if no URI like that in docList, otherwise return the hash of the doc you deleted.
        if (format == null) { throw new IllegalArgumentException(); }
        Document document = null;
        try {
            byte[] docInBytes = new byte[input.available()];
            input.read(docInBytes);
            document = processDoc(docInBytes, uri, format);//SINCE THIS HAPPENS EARLY IT MIGHT NOT BE GOOD TO DO TRIE IN HERE
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (hTable.get(uri) != null) {//MODIFY A DOC LOGIC
            if (hTable.get(uri).getDocumentTextHashCode() == document.getDocumentTextHashCode()) {hTable.get(uri).setLastUseTime(System.nanoTime()); useTimeMinHeap.reHeapify(hTable.get(uri));}
                
                //we know doc is not null b/c if Uri's value is not null then there must be a value by that node because if we ever put something with a null value we'd just delete it
            //since the texts are the same there is no need to modify - thus the body is empty.
            else {
                Document finalDocument = getDocument(uri);//type of its interface.Here we hold onto a reference to the old version of doc just before we change it.
                minHeapMod(finalDocument, document);
                calcAfterDel(finalDocument);//by modifying you are "deleting" the old doc and replacing it w/ new one.
                calcBeforePut(document);
                hTable.put(uri, document);//modify doc
                removeDocFromTrie(finalDocument);//processDoc() puts the new words into trie
                commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                    minHeapMod(hTable.get(uri1), finalDocument);
                    calcAfterDel(hTable.get(uri1));
                    removeDocFromTrie(hTable.get(uri1));
                    calcBeforePut(finalDocument);
                    hTable.put(uri1, finalDocument);//is this a reference. yes docList.put only reassigns the nodes value reference it does not set the old value to null. which means it still exists as long as we have a reference.

                    //this should return true if the document after the undo(@ that loc. in HashT) is the EXACT same doc(same loc.) as finalDocument.
                    //Shouldnt we remove from the trie the doc were replacing?
                    putDocInTrie(finalDocument);//Since the undo uses hTable's put and not DocStores, we need to put the words back in the trie here.
                    return hTable.get(uri1).equals(finalDocument);
                }));//this undo sets the document back to what it was before the modification.
                return document.getDocumentTextHashCode();
            }
        } else {//PUT IN A NEW DOC LOGIC
            calcBeforePut(document);
            hTable.put(uri, document);
            document.setLastUseTime(System.nanoTime());
            useTimeMinHeap.insert(document);
            commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                Document tempRef = hTable.get(uri1);
                hTable.put(uri1, null);
                tempRef.setLastUseTime(Long.MIN_VALUE);
                useTimeMinHeap.reHeapify(tempRef);
                useTimeMinHeap.removeMin();
                calcAfterDel(tempRef);
                removeDocFromTrie(tempRef);
                return hTable.get(uri1) == null;
            }));
        }
        return document.getDocumentTextHashCode();//will not be null Same reason as above(line 37 - though could have changed)
    }
    private void minHeapMod(Document oldDoc,Document newDoc){
        oldDoc.setLastUseTime(Long.MIN_VALUE);
        useTimeMinHeap.reHeapify(oldDoc);
        useTimeMinHeap.removeMin();
        newDoc.setLastUseTime(System.nanoTime());
        useTimeMinHeap.insert(newDoc);
    }

    public byte[] getDocumentAsPdf(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (hTable.get(uri) == null) {
            return null;
        }
        Document doc = (Document) hTable.get(uri);
        doc.setLastUseTime(System.nanoTime());
        useTimeMinHeap.reHeapify(doc);
        return doc.getDocumentAsPdf();
    }

    public String getDocumentAsTxt(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (hTable.get(uri) == null) {
            return null;
        }
        Document doc = (Document) hTable.get(uri);
        doc.setLastUseTime(System.nanoTime());
        useTimeMinHeap.reHeapify(doc);
        return doc.getDocumentAsTxt();
    }

    public boolean deleteDocument(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (hTable.get(uri) == null) {
            return false;
        } else {
            boolean exists = hTable.get(uri) != null;
            Document tempRef = hTable.get(uri);
            hTable.put(uri, null);//this calls hTables put - not docStores.
            removeDocFromTrie(tempRef);
            tempRef.setLastUseTime(Long.MIN_VALUE);
            useTimeMinHeap.reHeapify(tempRef);
            useTimeMinHeap.removeMin();
            calcAfterDel(tempRef);
            commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                calcBeforePut(tempRef);
                hTable.put(uri1, tempRef);
                if (tempRef.equals(hTable.get(uri))) {//MAKE SURE EQUALS METHOD IS LOGICALLY CORRECT//this is only if they are exactly the same object
                    putDocInTrie(tempRef);//reput the words of the doc in back in the trie
                    tempRef.setLastUseTime(System.nanoTime());
                    useTimeMinHeap.insert(tempRef);
                    return true;
                } else {
                    return false;
                }
            }));
            exists = hTable.get(uri) == null && exists;
            return exists;
        }
    }

    protected Document getDocument(URI uri) {
        return hTable.get(uri);
    }

    @Override
    public void undo() throws IllegalStateException {
        if (commandStack.peek() == null) {
            throw new IllegalStateException();
        }
        commandStack.peek().undo();
        commandStack.pop();//take off the command we just undid. EX PutDocX is a command at the top of the stack. we undo it(command.undo()/delete docX). now POP that command off stack - we just undid it.
        //IDK about this --> since the undo you do adds either a delete action or put action we need to get rid of it in order for an undo to actually make its way down stack.
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        if (commandStack.size() == 0) {
            throw new IllegalStateException();
        }
        StackImpl<Undoable> tempStack = new StackImpl<>();
        Undoable tempVar = null;
        do {
            tempVar = commandStack.pop();
            if(tempVar instanceof CommandSet){
                if(((CommandSet) tempVar).containsTarget(uri)) {
                    ((CommandSet) tempVar).undo(uri);
                    if(((CommandSet) tempVar).size() != 0){commandStack.push(tempVar);}
                    tempVar = null;
                }
                if(tempVar != null && ((CommandSet)tempVar).size() == 0){tempStack.push(tempVar);}
            }
            else if(uri.equals(((GenericCommand<URI>)tempVar).getTarget())) {
                tempVar.undo();//this undo adds another
                //Might need to pop off the undo action//commandStack.pop() does that account for elements/size, does any undo account?
                tempVar = null;
            }
            else { tempStack.push(tempVar); }
        } while (tempVar != null && commandStack.size() != 0);
        while (tempStack.size() != 0) {
            commandStack.push(tempStack.pop());
        }
    }

    /*
     *returns a list of strings because user want bodies of text, not Document - what can they do with that!
     */
    @Override
    public List<String> search(String keyword) {
        if(keyword == null){return new ArrayList<String>();}
        List<String> sortedList = new ArrayList<>();
        List<Document> list = searchTree.getAllSorted(keyword, (Document doc1, Document doc2) -> {return doc2.wordCount(keyword) - doc1.wordCount(keyword);});
        long lastUseTime = System.nanoTime();
        for (Document d : list) {
            sortedList.add(d.getDocumentAsTxt());
            d.setLastUseTime(lastUseTime);
            useTimeMinHeap.reHeapify(d);
        }
        return sortedList;
    }
    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if(keyword == null){return new ArrayList<byte[]>();}
        List<byte[]> sortedList = new ArrayList<>();
        List<Document> list = searchTree.getAllSorted(keyword, (Document doc1, Document doc2) -> {return doc2.wordCount(keyword) - doc1.wordCount(keyword);});
        long lastUseTime = System.nanoTime();
        for (Document d : list) {
            sortedList.add(d.getDocumentAsPdf());
            d.setLastUseTime(lastUseTime);
            useTimeMinHeap.reHeapify(d);
        }
        return sortedList;
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if(keywordPrefix == null){return new ArrayList<String>();}
        List<String> sortedList = new ArrayList<>();
        List<Document> list = searchTree.getAllWithPrefixSorted(keywordPrefix, makeComparator(keywordPrefix));
        long lastUseTime = System.nanoTime();
        for (Document d : list) {
            sortedList.add(d.getDocumentAsTxt());
            d.setLastUseTime(lastUseTime);
            useTimeMinHeap.reHeapify(d);
        }
        return sortedList;
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if(keywordPrefix == null){return new ArrayList<byte[]>();}
        List<byte[]> sortedList = new ArrayList<>();
        List<Document> list = searchTree.getAllWithPrefixSorted(keywordPrefix, makeComparator(keywordPrefix));
        long lastUseTime = System.nanoTime();
        for (Document d : list) {
            sortedList.add(d.getDocumentAsPdf());
            d.setLastUseTime(lastUseTime);
            useTimeMinHeap.reHeapify(d);
        }
        return sortedList;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        if (keyword == null) {return new HashSet<URI>();}
        Set<Document> docToDelete = searchTree.deleteAll(keyword);
        return docStoreDel(docToDelete);
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if(keywordPrefix == null){return new HashSet<URI>();}//technically redundant since delallw/prefx takes care of it.
        Set<Document> docToDelete = searchTree.deleteAllWithPrefix(keywordPrefix);
        return docStoreDel(docToDelete);
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        this.maxCount = limit;
        maxCountSet = true;
        while(this.currentCount > this.maxCount){ freeUpMemory();}
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        this.maxBytes = limit;
        maxBytesSet = true;
        while(this.currentBytes > this.maxBytes){ freeUpMemory();}
    }

    private DocumentImpl processDoc(byte[] docInBytes, URI uri, DocumentStore.DocumentFormat format) {
        String text1 = "";
        DocumentImpl document = null;
        try {
            if (format.equals(DocumentStore.DocumentFormat.PDF)) {
                PDDocument pdDocument = PDDocument.load(docInBytes);
                PDFTextStripper textStripper = new PDFTextStripper();
                text1 = textStripper.getText(pdDocument);
                pdDocument.close();//new
                text1 = text1.replace("\n", "");
                text1 = text1.replace("\r", "");
                document = new DocumentImpl(uri, text1, text1.hashCode(), docInBytes);
            } else if (format.equals(DocumentStore.DocumentFormat.TXT)) {
                text1 = new String(docInBytes);
                document = new DocumentImpl(uri, text1, text1.hashCode());
            } else {
                throw new IllegalArgumentException();
            }//if no doc format is valid
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }
        for (String word : text1.split(" ")) {
            searchTree.put(word, document);
        }
        return document;
    }
    private Set<URI> docStoreDel(Set<Document> docs) {
        CommandSet<URI> commandSet = new CommandSet<>();//cmdset of generic commands with URI type pass
        Set<URI> uriSet = new HashSet<>();
        long lastUseTime = System.nanoTime();
        for (Document d : docs) {
            for (String word : ((DocumentImpl) d).getWordMap().keySet()) {//STEP 1 DELETE DOC D COMPLETELY FROM TRIE
                searchTree.delete(word, d);
            }
            URI currentUri = d.getKey();
            Document tempRef = hTable.get(currentUri);
            hTable.put(d.getKey(), null);//STEP 2 DELETE DOC D FROM HASHTABLE
            tempRef.setLastUseTime(Long.MIN_VALUE);
            useTimeMinHeap.reHeapify(tempRef);
            useTimeMinHeap.removeMin();
            calcAfterDel(tempRef);
            commandSet.addCommand(new GenericCommand<URI>(currentUri, uri1 -> {
                calcBeforePut(tempRef);
                hTable.put(uri1, tempRef);
                if (tempRef.equals(hTable.get(uri1))) {
                    putDocInTrie(tempRef);
                    tempRef.setLastUseTime(lastUseTime);
                    useTimeMinHeap.insert(tempRef);

                    return true;
                } else {
                    return false;
                }
            }));
            uriSet.add(d.getKey());
        }
        commandStack.push(commandSet);//if nothing is deleted you will still add an(empty) cmdSet on the stack.
        return uriSet;
    }
    private void putDocInTrie(Document doc){
        for(String word: doc.getDocumentAsTxt().split(" ")){
            if(!(word.length() == 0)) {
                searchTree.put(word, doc);
            }
        }
    }
    private void removeDocFromTrie(Document doc){
        for(String word: doc.getDocumentAsTxt().split(" ")){
            if(!(word.length() == 0)) {//make sure words are coming in and not whitespace- this deals with situation of double spacing
                searchTree.delete(word, doc);
            }
        }
    }
    private Comparator<Document> makeComparator(String pkey) {
        return (Document doc1, Document doc2) -> {
            int prefixCountDoc1 = 0;
            int prefixCountDoc2 = 0;
            String newKeyPrefix = pkey.toUpperCase().replaceAll("[^0-9A-Z]", "");
            for (String key : ((DocumentImpl) doc2).getWordMap().keySet()) {
                if (key.startsWith(newKeyPrefix)) {
                    {
                        prefixCountDoc2 += ((DocumentImpl) doc2).getWordMap().get(key);
                    }
                }
            }
            for (String key : ((DocumentImpl) doc1).getWordMap().keySet()) {
                if (key.startsWith(newKeyPrefix)) {
                    prefixCountDoc1 += ((DocumentImpl) doc1).getWordMap().get(key);
                }
            }
            return prefixCountDoc2 - prefixCountDoc1;
        };
    }
    
    private void calcAfterDel(Document doc){
        this.currentCount--;
        this.currentBytes -= doc.getDocumentAsTxt().getBytes().length + doc.getDocumentAsPdf().length;
    }
    private void calcBeforePut(Document doc){
        this.currentCount++;
        this.currentBytes +=  doc.getDocumentAsTxt().getBytes().length + doc.getDocumentAsPdf().length;
        if(maxCountSet){ while(this.currentCount > this.maxCount){ this.freeUpMemory(); } }
        if(maxBytesSet){ while(this.currentBytes > this.maxBytes){ this.freeUpMemory(); } }//dont need to worry bout double delete b/c free up memory calculates after delete
    }
    private void freeUpMemory(){
        Document docToRemove = useTimeMinHeap.removeMin();
        hTable.put(docToRemove.getKey(), null);
        removeDocFromTrie(docToRemove);
        calcAfterDel(docToRemove);
        removeDocFromCmdStack(docToRemove.getKey());
    }
    private void removeDocFromCmdStack(URI uri){
        StackImpl<Undoable> tempStack = new StackImpl<>();
        Undoable tempVar = null;
        while(this.commandStack.size() != 0) {
            tempVar = commandStack.pop();
            if (tempVar instanceof CommandSet) {
                if (((CommandSet) tempVar).size() != 0 && ((CommandSet) tempVar).containsTarget(uri)) {
                    Iterator<GenericCommand<URI>> iterator = ((CommandSet) tempVar).iterator();
                    while (iterator.hasNext()) {
                        if (uri.equals(iterator.next().getTarget())) {
                            iterator.remove();
                            break;//there shouldnt be only one matching uri in cmdSet
                        }
                    }
                    if (((CommandSet) tempVar).size() != 0) {
                        tempStack.push(tempVar);
                    }
                } else {
                    tempStack.push(tempVar);
                }
            } else if (uri.equals(((GenericCommand) tempVar).getTarget())) {
                //do nothing - leave it popped off the cmdStack
            }
            else{tempStack.push(tempVar);}
        }
        while (tempStack.size() != 0) {
            commandStack.push(tempStack.pop());
        }
    }
}
