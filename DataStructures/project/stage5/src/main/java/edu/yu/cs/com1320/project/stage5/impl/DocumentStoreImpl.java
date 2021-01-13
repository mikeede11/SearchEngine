package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    static enum DocumentFormat {PDF, TXT}

    private BTreeImpl<URI, Document> bTree = new BTreeImpl<>();
    private StackImpl<Undoable> commandStack = new StackImpl<>();
    private TrieImpl<URI> searchTree = new TrieImpl<>();
    private MinHeap<URI> useTimeMinHeap = new MinHeapImplExt<>();
    private int maxCount;
    private int maxBytes;
    private int currentCount = 0;
    private int currentBytes = 0;
    private boolean maxCountSet;
    private boolean maxBytesSet;

    public DocumentStoreImpl() {
        bTree.setPersistenceManager(new DocumentPersistenceManager(new File(System.getProperty("user.dir"))));
        URI sentinel = null;
        try {
            sentinel = new URI("http://A");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        bTree.put(sentinel, null);
        ((MinHeapImplExt)useTimeMinHeap).setBTree(bTree);
    }
    public DocumentStoreImpl(File baseDir){
        bTree.setPersistenceManager(new DocumentPersistenceManager(baseDir));
        URI sentinel = null;
        try {
            sentinel = new URI("http://A");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        bTree.put(sentinel, null);
        ((MinHeapImplExt)useTimeMinHeap).setBTree(bTree);
    }

    public int putDocument(InputStream input, URI uri, DocumentStore.DocumentFormat format) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            if (bTree.get(uri) == null) {//you can use it here and not update it b/c if its null it shouldnt be updated(it was already deleted) - if its valid it will be updated later(referring to minHeap)
                return 0;
            }
            int docHashCode = bTree.get(uri).getDocumentTextHashCode();//your deleting it no need to update it;
            deleteDocument(uri);//TODO CHECK LOGIC
            return docHashCode;
        }//b/c if User inputs null we are supposed to delete document. return 0 if no URI like that in docList, otherwise return the hash of the doc you deleted.
        if (format == null) {
            throw new IllegalArgumentException();
        }
        Document newDocument = null;
        try {
            byte[] docInBytes = new byte[input.available()];
            input.read(docInBytes);
            newDocument = processDoc(docInBytes, uri, format);//SINCE THIS HAPPENS EARLY IT MIGHT NOT BE GOOD TO DO TRIE IN HERE
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bTree.get(uri) != null) {//MODIFY A DOC LOGIC(can call bTree.get here b/c the three paths it can take at this point will update it.
            if (getDocAndUpdateMinHeap(uri).getDocumentTextHashCode() == newDocument.getDocumentTextHashCode()) {
                return newDocument.getDocumentTextHashCode();//there the same it doesnt matter which;
            }//no need to do anything simply looking for it updated the doc.
            //we know doc is not null b/c if Uri's value is not null then there must be a value by that node because if we ever put something with a null value we'd just delete it
            //since the texts are the same there is no need to modify - thus the body is empty.
            else {
                Document oldDoc = bTree.get(uri);//type of its interface.Here we hold onto a reference to the old version of doc just before we change it.
                removeDocFromTrie(oldDoc);//YOUADDED THESE FOUR LINES HERE AND TOOK AWAY REMOVEDOC FROM TRIE BELOW.THESE ARE REPLACING THE THE PUTS IN TRIE MOST NOTABLY ORIGINALLY IN PROCESSDOC
                putDocInTrie(newDocument);
                minHeapMod(newDocument);
                calcAfterDel(oldDoc);//by modifying you are "deleting" the old doc and replacing it w/ new one.
                calcBeforePut(newDocument);
                bTree.put(uri, newDocument);//modify doc
                Document finalNewDocument = newDocument;
                commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                    minHeapMod(oldDoc);
                    removeDocFromTrie(finalNewDocument);
                    calcAfterDel(finalNewDocument);
                    calcBeforePut(oldDoc);
                    bTree.put(uri1, oldDoc);//is this a reference. yes docList.put only reassigns the nodes value reference it does not set the old value to null. which means it still exists as long as we have a reference.
                    //this should return true if the document after the undo(@ that loc. in HashT) is the EXACT same doc(same loc.) as finalDocument.
                    //Shouldnt we remove from the trie the doc were replacing?
                    putDocInTrie(oldDoc);//Since the undo uses bTrees's put and not DocStores, we need to put the words back in the trie here.
                    return bTree.get(uri1).equals(oldDoc);
                }));//this undo sets the document back to what it was before the modification.
                return oldDoc.getDocumentTextHashCode();
            }
        } else {//PUT IN A NEW DOC LOGIC
            putDocInTrie(newDocument);
            calcBeforePut(newDocument);
            bTree.put(uri, newDocument);
            newDocument.setLastUseTime(System.nanoTime());
            useTimeMinHeap.insert(newDocument.getKey());
            commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                Document tempDoc = minHeapDelete(uri1);
                /*Document tempDoc = bTree.get(uri1);
                //bTree.put(uri1, null);
                tempDoc.setLastUseTime(Long.MIN_VALUE);
                useTimeMinHeap.reHeapify(tempDoc.getKey());
                useTimeMinHeap.removeMin();*/
                bTree.put(uri1, null);
                calcAfterDel(tempDoc);
                removeDocFromTrie(tempDoc);
                return bTree.get(uri1) == null;
            }));
        }
        return 0;//newDocument.getDocumentTextHashCode();//will not be null Same reason as above(line 37 - though could have changed)
    }

    private Document getDocAndUpdateMinHeap(URI uri){//this method only deals with docs that are definitly in the Btree/or disk
        Document doc = bTree.get(uri);
        doc.setLastUseTime(System.nanoTime());
        if(((MinHeapImplExt)useTimeMinHeap).inMinHeap(uri)){//if its in MinHeap just update, if not you will add it below and memory manage b/c that means you brought it in from disk
            useTimeMinHeap.reHeapify(uri);
        }
        else {
            useTimeMinHeap.insert(uri);
            calcBeforePut(doc);//this isnt ideal but basically right after we might have put in a doc from disk we do memory management
        }
        return doc;
    }
    private Document minHeapDelete(URI uri){
        Document doc = bTree.get(uri);
        if(((MinHeapImplExt)useTimeMinHeap).inMinHeap(uri)){//if its in MinHeap just update, if not you will add it below and memory manage b/c that means you brought it in from disk
            doc.setLastUseTime(Long.MIN_VALUE);
            useTimeMinHeap.reHeapify(uri);
            useTimeMinHeap.removeMin();
            calcAfterDel(doc);
        }
        return doc;
    }

    private void minHeapMod(Document newDoc) {
        newDoc.setLastUseTime(System.nanoTime());
        useTimeMinHeap.reHeapify(newDoc.getKey());
    }

    public byte[] getDocumentAsPdf(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (bTree.get(uri) == null) {
            return null;
        }
        Document doc =  getDocAndUpdateMinHeap(uri);
        return doc.getDocumentAsPdf();
    }

    public String getDocumentAsTxt(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (bTree.get(uri) == null) {
            return null;
        }
        Document doc = getDocAndUpdateMinHeap(uri);
        return doc.getDocumentAsTxt();

    }

    public boolean deleteDocument(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (bTree.get(uri) == null) {//if it is null nothing we need to do. if it isnt then the logic below will deal.
            return false;
        } else {
            boolean exists = bTree.get(uri) != null;//even though we keep calling btree.get we will deal with it.
            Document tempRef = minHeapDelete(uri);
            bTree.put(uri, null);//this calls hTables put - not docStores.
            removeDocFromTrie(tempRef);
            commandStack.push(new GenericCommand<URI>(uri, uri1 -> {
                calcBeforePut(tempRef);
                bTree.put(uri1, tempRef);
                if (tempRef.equals(bTree.get(uri))) {//MAKE SURE EQUALS METHOD IS LOGICALLY CORRECT//this is only if they are exactly the same object//ok for Btree.get to be here same reason i said in docStoreDel()
                    putDocInTrie(tempRef);//reput the words of the doc in back in the trie
                    tempRef.setLastUseTime(System.nanoTime());
                    useTimeMinHeap.insert(tempRef.getKey());
                    return true;
                } else {
                    return false;
                }
            }));
            exists = bTree.get(uri) == null && exists;
            return exists;
        }
    }

    protected Document getDocument(URI uri) {
        Document doc = null;
        if (((MinHeapImplExt)useTimeMinHeap).inMinHeap(uri)) {//If its in the MinHeap, its in memory - return the actual doc
            doc = bTree.get(uri);
        }
            return doc;//if it was in memory the if statement above will get right document
            // - otherwise null to signal on disk or deleted.
    }

    @Override
    public void undo() throws IllegalStateException {
        if (commandStack.size() == 0) {
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
            if (tempVar instanceof CommandSet) {
                if (((CommandSet) tempVar).containsTarget(uri)) {
                    ((CommandSet) tempVar).undo(uri);
                    if (((CommandSet) tempVar).size() != 0) {
                        commandStack.push(tempVar);
                    }
                    tempVar = null;
                }
                if (tempVar != null && ((CommandSet) tempVar).size() == 0) {
                    tempStack.push(tempVar);
                }
            } else if (uri.equals(((GenericCommand<URI>) tempVar).getTarget())) {
                tempVar.undo();//this undo adds another
                //Might need to pop off the undo action//commandStack.pop() does that account for elements/size, does any undo account?
                tempVar = null;
            } else {
                tempStack.push(tempVar);
            }
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
        if (keyword == null) {
            return new ArrayList<String>();
        }
        List<String> sortedList = new ArrayList<>();
        List<URI> list = searchTree.getAllSorted(keyword, (URI uri1, URI uri2) -> bTree.get(uri2).wordCount(keyword) - bTree.get(uri1).wordCount(keyword));
        long lastUseTime = System.nanoTime();
        for (URI u : list) {
            sortedList.add(getDocAndUpdateMinHeap(u).getDocumentAsTxt());
        }
        return sortedList;
    }

    @Override
    public List<byte[]> searchPDFs(String keyword) {
        if (keyword == null) {
            return new ArrayList<>();
        }
        List<byte[]> sortedList = new ArrayList<>();
        List<URI> list = searchTree.getAllSorted(keyword, (URI uri1, URI uri2) -> {
            return bTree.get(uri2).wordCount(keyword) - bTree.get(uri1).wordCount(keyword);
        });
        for (URI u : list) {
            sortedList.add(getDocAndUpdateMinHeap(u).getDocumentAsPdf());
        }
        return sortedList;
    }

    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new ArrayList<>();
        }
        List<String> sortedList = new ArrayList<>();
        List<URI> list = searchTree.getAllWithPrefixSorted(keywordPrefix, makeComparator(keywordPrefix));
        for (URI u : list) {
            sortedList.add(getDocAndUpdateMinHeap(u).getDocumentAsTxt());
        }
        return sortedList;
    }

    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new ArrayList<>();
        }
        List<byte[]> sortedList = new ArrayList<>();
        List<URI> list = searchTree.getAllWithPrefixSorted(keywordPrefix, makeComparator(keywordPrefix));
        for (URI u : list) {
            sortedList.add(getDocAndUpdateMinHeap(u).getDocumentAsPdf());
        }
        return sortedList;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        if (keyword == null) {
            return new HashSet<URI>();
        }
        Set<URI> docToDelete = searchTree.deleteAll(keyword);
        return docStoreDel(docToDelete);
    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        if (keywordPrefix == null) {
            return new HashSet<URI>();
        }//technically redundant since delallw/prefx takes care of it.
        Set<URI> docToDelete = searchTree.deleteAllWithPrefix(keywordPrefix);
        return docStoreDel(docToDelete);
    }

    private void calcAfterDel(Document doc) {
        this.currentCount--;
        this.currentBytes -= doc.getDocumentAsTxt().getBytes().length + doc.getDocumentAsPdf().length;
    }

    private void calcBeforePut(Document doc) {
        this.currentCount++;
        this.currentBytes += doc.getDocumentAsTxt().getBytes().length + doc.getDocumentAsPdf().length;
        if (maxCountSet) {
            while (this.currentCount > this.maxCount) {
                this.freeUpMemory();
            }
        }
        if (maxBytesSet) {
            while (this.currentBytes > this.maxBytes) {
                this.freeUpMemory();
            }
        }//dont need to worry bout double delete b/c free up memory calculates after delete
    }

    private void freeUpMemory() {
        URI uriToRemove = useTimeMinHeap.removeMin();
        Document doc = bTree.get(uriToRemove);//this is valid b/c its still in memory so your not changing anything. just keeping a pointer
        try {
            bTree.moveToDisk(uriToRemove);
        } catch (Exception e) {
            e.printStackTrace();
        }
        calcAfterDel(doc);
        doc = null;//no doc memory is on RAM now
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        this.maxCount = limit;
        maxCountSet = true;
        while (this.currentCount > this.maxCount) {
            freeUpMemory();
        }
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        this.maxBytes = limit;
        maxBytesSet = true;
        while (this.currentBytes > this.maxBytes) {
            freeUpMemory();
        }
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
            e.printStackTrace();
        }
        return document;
    }

    private Set<URI> docStoreDel(Set<URI> uris) {
        CommandSet<URI> commandSet = new CommandSet<>();//cmdset of generic commands with URI type pass
        Set<URI> uriSet = new HashSet<>();
        long lastUseTime = System.nanoTime();
        for (URI u : uris) {
            for (String word : bTree.get(u).getWordMap().keySet()) {//STEP 1 DELETE DOC D COMPLETELY FROM TRIE - (ok to use Btree.get here, logic below will update minHeap accordingly)
                searchTree.delete(word, u);
            }
            URI currentUri = u;//not really necessary
            Document tempRef = minHeapDelete(currentUri);
            bTree.put(currentUri, null);//STEP 2 DELETE DOC D FROM BTREE
            commandSet.addCommand(new GenericCommand<URI>(currentUri, uri1 -> {
                calcBeforePut(tempRef);
                bTree.put(uri1, tempRef);
                if (tempRef.equals(bTree.get(uri1))) {//it will be updated below, but if its not something is wrong anyways.it should always return true for this project.
                    putDocInTrie(tempRef);
                    tempRef.setLastUseTime(lastUseTime);
                    useTimeMinHeap.insert(tempRef.getKey());
                    return true;
                } else {
                    return false;
                }
            }));
            uriSet.add(u);
        }
        commandStack.push(commandSet);//if nothing is deleted you will still add an(empty) cmdSet on the stack.
        return uriSet;
    }

    private void putDocInTrie(Document doc) {
        for (String word : doc.getDocumentAsTxt().split(" ")) {
            if (!(word.length() == 0)) {
                searchTree.put(word, doc.getKey());
            }
        }
    }

    private void removeDocFromTrie(Document doc) {
        for (String word : doc.getDocumentAsTxt().split(" ")) {
            if (!(word.length() == 0)) {//make sure words are coming in and not whitespace- this deals with situation of double spacing
                searchTree.delete(word, doc.getKey());
            }
        }
    }

    private Comparator<URI> makeComparator(String pkey) {//ALL THE BTREE.GETS in this method can be here, because the comparator is only used in search methods that update minHeap for all those documents accordingly.btree
        return (URI uri1, URI uri2) -> {
            int prefixCountDoc1 = 0;
            int prefixCountDoc2 = 0;
            String newKeyPrefix = pkey.toUpperCase().replaceAll("[^0-9A-Z]", "");
            for (String key : bTree.get(uri2).getWordMap().keySet()) {
                if (key.startsWith(newKeyPrefix)) {
                    {
                        prefixCountDoc2 += bTree.get(uri2).getWordMap().get(key);
                    }
                }
            }
            for (String key : bTree.get(uri1).getWordMap().keySet()) {
                if (key.startsWith(newKeyPrefix)) {
                    prefixCountDoc1 += bTree.get(uri1).getWordMap().get(key);
                }
            }
            return prefixCountDoc2 - prefixCountDoc1;
        };
    }
}