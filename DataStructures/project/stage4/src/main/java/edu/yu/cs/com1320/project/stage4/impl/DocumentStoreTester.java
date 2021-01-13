package edu.yu.cs.com1320.project.stage4.impl;

import java.lang.reflect.Type;
import java.net.URI;

import com.google.gson.*;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.sun.javafx.runtime.SystemProperties;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import jdk.nashorn.internal.parser.JSONParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DocumentStoreTester {

    public static void main(String[] args) {
//Makes a PDF Doc to test
        PDDocument pdfDoc = new PDDocument();
        PDPage page = new PDPage();
        pdfDoc.addPage(page);
        InputStream is = null;
        ByteArrayOutputStream out = null;
        String text = "This is a super effective PDF PDF PDF PDF PDF PDF Document sea shells shore she";
        byte[] pdfInfo = null;
        try {
            PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
            contentStream.beginText();
            contentStream.newLineAtOffset(25, 700);
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(text);
            contentStream.endText();
            contentStream.close();
            //this is getting the PDF Doc into a stream tiasepd
            out = new ByteArrayOutputStream();
            pdfDoc.save(out);
            pdfInfo = out.toByteArray();
            URI pdfUri = new URI("thisisapdfuri");
            ByteArrayInputStream is1 = new ByteArrayInputStream(pdfInfo);
            DocumentStoreImpl store = new DocumentStoreImpl();
            //tests if putDocument is returning the correct HashCode
            if (store.search("anyword").size() == 0) {
                System.out.println("The trie is able to deal with a case when nothing has been put in the trie - we can assume this is the same for searchPDFs");
            } else {
                System.out.println("FAILURE!");
            }
            // TODO: 4/8/2020 MAKE TESTS FOR MODIFICATION PUTTING NEW VERSION IN TRIE AND DELETING OLD VERSION OF DOC
            // todo : when you delete documents with any method or with any undo you have to delete the docs in the trie
            // todo ALSO We have proven that upon an undo of a deleteall the words are not added back in the trie - i assume this is so for all undos}
            if (store.deleteAll("NOACTUAL WORD").size() == 0) {
                System.out.println("the trie can handle a delete method with nothing in the trie(returns an empty set");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.putDocument(is1, pdfUri, DocumentStore.DocumentFormat.PDF) == text.hashCode()) {//print out hash code of variable text
                System.out.println("SUCCESS we put a PDF Doc in our store");
            } else {
                System.out.println("FAIL");
            }
            if (store.deleteAll("wordNot indoc store").size() == 0) {
                System.out.println("The doc store can take a delete that is not in the storeand return an empty set");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.deleteAllWithPrefix("Not a Word").size() == 0) {
                System.out.println("SAME WITH DELETE ALL WITH PREFIX");
            } else {
                System.out.println("FAILURE");
            }
            //Tests if getDocAstext() is returning the correct string
            if (store.getDocumentAsTxt(pdfUri).equals(text)) {
                System.out.println("SUCCESS we can get the pdf as txt");
            } else {
                System.out.println("FAIL");
            }
            store.deleteAll("super");//deletes pdf w/ cmdset
            //Tests if getDocumentAsPDF returns correct PDFBytes
            //PDDocument doc = PDDocument.load(store.getDocumentAsPdf(pdfUri));
            PDFTextStripper ts = new PDFTextStripper();
            // String newText = ts.getText(doc);*/
            //System.out.println(newText);//we see through the result and the debugger that this test has indeed passed
            File tempTxt = File.createTempFile("csDoc", ".txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempTxt));
            String uriTxtinput = "This is an super ORIGINAL Text Document, not a PDF!";
            writer.write(uriTxtinput);//
            writer.close();
            InputStream txtIs = new FileInputStream(tempTxt);
            URI txtUri = new URI("ThisIsATxtDoc");
            if (store.putDocument(txtIs, txtUri, DocumentStore.DocumentFormat.TXT) == uriTxtinput.hashCode()) {
                System.out.println("SUCCESS! getDocasPdf returns correct Bytes");
            } else {
                System.out.println("FAIL");
            }
            store.undo(pdfUri);
            //Tests If SearchPDFs retrieves the correct documents we just put in - and they must be in the correct order
            List<byte[]> pdfList = new ArrayList<byte[]>();
            pdfList.add(store.getDocumentAsPdf(pdfUri));//
            pdfList.add(store.getDocumentAsPdf(txtUri));//
            List<byte[]> testList = store.searchPDFs("pdf");
            for (int i = 0; i < pdfList.size(); i++) {
                if (testList.get(i).equals(pdfList.get(i))) {
                    System.out.println("Search for documents with the KeyWord PDF succeeded - and in the correct order- IN PDF FORMAT");
                } else {
                    System.out.println("FAILURE!");
                }
            }
            List<String> textList = new ArrayList<String>();
            textList.add(store.getDocumentAsTxt(pdfUri));//
            textList.add(store.getDocumentAsTxt(txtUri));//
            List<String> testList2 = store.search("pdf");
            for (int i = 0; i < textList.size(); i++) {
                if (testList2.get(i).equals(textList.get(i))) {
                    System.out.println("Search for documents with the KeyWord PDF succeeded - and in the correct order- in TXT FORMAT");
                } else {
                    System.out.println("FAILURE!");
                }
            }
            if (store.search("thisisnotawordinthestore").size() == 0) {
                System.out.println("Search for a word that was not in any of the documents in the TRIE succeeded!");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.search("p()*&^%$#@DF!!!!!!!!").get(0).equals(textList.get(0))) {
                System.out.println("The Search engine can succesfully filter out undesired(non-alphanumeric) characters and return the result as if those characters were not there in the first place!");
            } else {
                System.out.println("FAILURE!");
            }
            Set<URI> uriSet = store.deleteAll("pdf");
            Set<URI> testSet = new HashSet<URI>();
            testSet.add(pdfUri);
            testSet.add(txtUri);
            if (uriSet.equals(testSet)) {
                System.out.println("the DeleteAll Method of DocStore returns the correct URIs of the documents that should have been deleted!");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.getDocument(pdfUri) == null && store.getDocument(txtUri) == null) {
                System.out.println("DeleteAll successfully deleted the documents in the hashtable");
            } else {
                System.out.println("FAILURE!");
            }
            store.undo();
            testList2.clear();
            testList2.add(store.getDocumentAsTxt(pdfUri));
            testList2.add(store.getDocumentAsTxt(txtUri));
            for (int i = 0; i < textList.size(); i++) {
                if (testList2.get(i).equals(textList.get(i))) {
                    System.out.println("UNDO ALL SUCCEEDED WHICH IMPLIES COMMANDSET, COMMAND STACK IS WORKING! -BUT WHAT ABOUT THE TRIE?");
                } else {
                    System.out.println("FAILURE!");
                }
            }
            /*for (String s : store.search("pdf")) {
                System.out.println(s);
            }*/
            List<String> searchpdf = store.searchByPrefix("s");
            System.out.println(searchpdf.get(0));
            if (searchpdf.size() == 2) {
                System.out.println("searchByPrefix retrieved two docs ignoring duplicates");
            } else {
                System.out.println("Failure");
            }
            if (searchpdf.get(0).equals(store.getDocumentAsTxt(pdfUri)) && searchpdf.get(1).equals(store.getDocumentAsTxt(txtUri))) {
                System.out.println("BIG SUCCESS, we have made a comparator that is effective in ordering the documents from the search prefix methods");
            } else {
                System.out.println("fail");
            }
            List<byte[]> searchPdf2 = store.searchPDFsByPrefix("s");
            if (searchpdf.size() == 2) {
                System.out.println("searchPDFsByPrefix retrieved two docs ignoring duplicates");
            } else {
                System.out.println("Failure");
            }
            if (searchPdf2.get(0).equals(store.getDocumentAsPdf(pdfUri)) && searchPdf2.get(1).equals(store.getDocumentAsPdf(txtUri))) {
                System.out.println("BIG SUCCESS, we have made a comparator that is effective in ordering the documents from the search prefix methods");
            } else {
                System.out.println("fail");
            }
            //Test for text Doc modification
            File tempTxtMod = File.createTempFile("dataStrucDoc", ".txt");
            BufferedWriter writerMod = new BufferedWriter(new FileWriter(tempTxtMod));
            String uriTxtinputMod = "This is aj MODIFIED Text Document sea shells shore she!";
            writerMod.write(uriTxtinputMod);
            writerMod.close();
            InputStream txtIsMod = new FileInputStream(tempTxtMod);
            store.putDocument(txtIsMod, txtUri, DocumentStore.DocumentFormat.TXT);
            if (uriTxtinputMod.equals(store.getDocumentAsTxt(txtUri))) {
                System.out.println("SUCCESS- THE TXT DOC WAS MODIFIED - in the HashTable");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.search("MODIFIED").get(0).equals(store.getDocumentAsTxt(txtUri))) {
                System.out.println("When a document is Modified the TRIE is aware of it and it is searched the trie can deliver!");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.search("ORIGINAL").size() == 0) {
                System.out.println("The Document that was replaced by the modified version was succesfully removed from the trie upon modification");
            } else {
                System.out.println("FAILURE!");
            }
            List<String> prefixResultList = store.searchByPrefix("s");
            if (prefixResultList.size() == 2) {
                System.out.println("Search By prefix succesfully returned multiple documents with the given prefix, and at the same time filtered out duplicate documents at different keys(docs have many words in them(keys)");
            } else {
                System.out.println("FAILURE!");
            }
            if (store.deleteAllWithPrefix("s").size() == 2) {
                System.out.println("DeleteAllWithPrefix can successfully delete multiple documents at different Nodes");
            } else {
                System.out.println("FAILURE");
            }
            store.undo();
            //newTest for modification.
            PDDocument pdfDoc2 = new PDDocument();
            PDPage page2 = new PDPage();
            pdfDoc2.addPage(page2);
            InputStream isNew = null;
            ByteArrayOutputStream outNew = null;
            String textnew = "PDF content for doc2: PDF format was opened in 2008.";
            byte[] pdfInfoNew = null;
            PDPageContentStream contentStreamNew = new PDPageContentStream(pdfDoc2, page2);
            contentStreamNew.beginText();
            contentStreamNew.newLineAtOffset(25, 700);
            contentStreamNew.setFont(PDType1Font.TIMES_ROMAN, 12);
            contentStreamNew.newLineAtOffset(50, 700);
            contentStreamNew.showText(textnew);
            contentStreamNew.endText();
            contentStreamNew.close();
            //this is getting the PDF Doc into a stream
            outNew = new ByteArrayOutputStream();
            pdfDoc2.save(outNew);
            pdfInfoNew = outNew.toByteArray();
            ByteArrayInputStream baisNew = new ByteArrayInputStream(pdfInfoNew);
            store.putDocument(baisNew, pdfUri, DocumentStore.DocumentFormat.PDF);
            String textfromHT = store.getDocumentAsTxt(pdfUri);
            if (textnew.equals(textfromHT)) {
                System.out.println("SUCCESS - DOCUMENT AT PDFURI WAS SUCCESSFULLY MODIFIED");
            } else {
                System.out.println("FAILURE:{(");
            }
            if (store.deleteDocument(pdfUri) && store.search("2008").size() == 0 && store.getDocument(pdfUri) == null) {
                System.out.println("The Delete Document method removes the document from hashtable and from Trie");
            } else {
                System.out.println("FAILURE");
            }
            store.undo();
            if (store.search("2008").size() == 1 && store.getDocument(pdfUri) != null) {
                System.out.println("undo() puts a single genCommand back in HashTable and Trie");
            } else {
                System.out.println("Failure!");
            }
            store.deleteAllWithPrefix("pdf");
            store.undo(pdfUri);//should go through command stack and put pdf back in, otherwise it might ignore cmdset and do the cmd that modified it
            if (store.getDocumentAsTxt(pdfUri).equals(textnew) && store.search("2008").size() == 1) {
                System.out.println("The Undo(uri) can search through command sets in command stacks and undo the corresongding URI operation");
            } else {
                System.out.println("Failure!");
            }
            //This tests to see if we can get a PDF out of a txt file
            PDDocument txtDoc = PDDocument.load(store.getDocumentAsPdf(txtUri));
            if (ts.getText(txtDoc).equals(uriTxtinputMod + "\r\n")) {//this is because we know windows adds on \r \n. getTXT SHOULD DELETE THAT \R \N!!!
                System.out.println("SUCCESS!!!");
            } else {
                System.out.println("FAIL");
            }
            if (store.getDocumentAsTxt(txtUri).equals(uriTxtinputMod)) {
                System.out.println("SUCCESS:)");
            } else {
                System.out.println("FAIL");
            }
            //This code tests if the URI doesnt exist then these methods should return certain values.
            URI nullUri = new URI("thisURIshouldntWork");
            if (store.getDocumentAsPdf(nullUri) == null) {
                System.out.println("SUCCESS");
            } else {
                System.out.println("FAIL");
            }
            if (store.getDocumentAsTxt(nullUri) == null) {
                System.out.println("SUCCESS");
            } else {
                System.out.println("FAIL");
            }
            if (!(store.deleteDocument(nullUri))) {
                System.out.println("SUCCESS");
            } else {
                System.out.println("FAIL");
            }
            for (String s : store.search("pdf")) {
                System.out.println(s + "WOOHOOSUCCESS");
            }
            //TESTS IF DELETE MECHANISMS ARE EFFECTIVE
            if (store.deleteDocument(txtUri) && store.getDocumentAsTxt(txtUri) == null) {
                System.out.println("DELETESUCCESS!!");
            } else {
                System.out.println("FAIL");
            }
            store.putDocument(null, pdfUri, DocumentStore.DocumentFormat.PDF);
            if (store.getDocumentAsPdf(pdfUri) == null && store.getDocumentAsTxt(pdfUri) == null) {
                System.out.println("SUCCESS!");
            } else {
                System.out.println("FAIL");
            }
            //TESTS UNDO(URI) FUNCTION
            store.undo(pdfUri);
            if (textnew.equals(store.getDocumentAsTxt(pdfUri))) {
                System.out.println("SUCCESS" + store.getDocumentAsTxt(pdfUri));
            } else {
                System.out.println("FAILURE - DID NOT UNDO spec. uri");
            }
            //redo(return state before we undid so w can test more
            store.putDocument(null, pdfUri, DocumentStore.DocumentFormat.PDF);
            store.undo(txtUri);
            if (uriTxtinputMod.equals(store.getDocumentAsTxt(txtUri))) {
                System.out.println("SUCCESS - UNDO(URI) CAN UNDO A COMMAND IN THE MIDDLE OF THE STACK");
            } else {
                System.out.println("FAILURE");
            }
            store.deleteDocument(txtUri);//return to regular state
            //test an undo on a modification.
            store.undo(pdfUri);//undeletes it
            store.undo(pdfUri);//demodifies it
            if (text.equals(store.getDocumentAsTxt(pdfUri))) {
                System.out.println("SUCCESS UNDO CAN DEMODIFY A DOCUMENT REPLACING THE FORMER CONTENT");
            } else {
                System.out.println("FAILURE");
            }
            //wazzup - extra tests.
            store.undo();
            if (store.getDocumentAsTxt(txtUri) != null) {
                System.out.println("SUCCESS" + store.getDocumentAsTxt(txtUri));
            }
            store.undo();
            store.deleteAllWithPrefix("");//clear store//currentCount = 0
            store.setMaxDocumentBytes(1000);
            store.setMaxDocumentCount(1);
            store.undo(txtUri);//current count = 1
            if (store.getDocumentAsTxt(txtUri) != null) {
                System.out.println("the txt doc was put in the doc store which has " + store.getDocumentAsPdf(txtUri).length + " Bytes of data which is lower than 1000");
            } else {
                System.out.println("Failure");
            }
            store.undo(pdfUri);//adding a second doc which will go over the memory limit.//this is an undo of a deleteAll(cmdSet) - this covers deleteall and deleteallwithprefix (samecode)
            if (store.getDocumentAsTxt(txtUri) == null && store.getDocumentAsTxt(pdfUri) != null) {
                System.out.println("The doc store was able to succesfully delete the least recently used document to free up memory, while leaving the recent docs in that can fit. ");
            } else {
                System.out.println("Fail!");
            }
            //current count  = 1
            //Now we test if the memory capacity function works when we add a new doc with the put method.
            URI stage4Uri = new URI("Thisisastage4texttesturi");
            String stage4text = "This is a stage 4 Text Document sea shells shore she!";
            store.setMaxDocumentBytes(1500);
            store.setMaxDocumentCount(1);
            store.putDocument(makeAnewdoc("dataStrucDocStage4", stage4text), stage4Uri, DocumentStore.DocumentFormat.TXT);//this will go over max, the pdfdoc should be deleted, one cmd should be deleted and one shall be added on vis this put.
            if (store.getDocumentAsTxt(pdfUri) == null && store.getDocumentAsTxt(stage4Uri).equals(stage4text) && store.search("PDF").size() == 0) {
                System.out.println("Success for a new put in the doc store via the put method. the trie seems to be emptied of that doc. look at debugger for cmdstack. appropriate docs were deleted and added to hashtable ");
            } else {
                System.out.println("FAIL");
            }
            //current count = 1
            //now we test if memory capacity function works when we add a modification that is larger enough than the one it is replacing  so that the memory limit is surpassed - tricky.
            String stage4textMod = "this is a stage 4 text mod for a text document that takes up alot of bytes in order to make it go past cpacity to see how the stage 4 mechanism will deal with and addition of a modiifcation that makes the current bytes or count go over the max";
            URI uri4 = new URI("uri4woohoo");
            store.setMaxDocumentBytes(2000);//this is very precise b/c the modification will cause the NET bytes to go over 2000(NET = allOtherDocsBytes + (NewDocBytes - Old Doc Bytes))
            store.setMaxDocumentCount(1);//modification not big deal count is the same.
            //line below we make a new doc b/c we want somthing in the doc store to be able to be deleted if mem limit is reached(all other docs were deleted)
            store.putDocument(makeAnewdoc("somthingrandom", "very short document"), uri4, DocumentStore.DocumentFormat.TXT);
            //here we modify the document which will cause the memory limit to be surpassed. currentcount = 2
            store.putDocument(makeAnewdoc("dataStrucDocStage43", stage4textMod), stage4Uri, DocumentStore.DocumentFormat.TXT);
            if (store.getDocumentAsTxt(uri4) == null && store.getDocumentAsTxt(stage4Uri).equals(stage4textMod) && store.search("sea").size() == 0 && store.search("alot").size() == 1) {
                System.out.println("Big Success - upon putting in a modified version of a document that caused the memory limit to be surpassed, the doc store modified the doc appropriately deleted the oldest doc from the Htable and the Trie");
            } else {
                System.out.println("Fail");
            }
            //current Bytes = 1224, count = 1
            store.setMaxDocumentBytes(3000);
            store.setMaxDocumentCount(2);
            URI uri5 = new URI("thisisurinumberfive");
            String text5 = "this Document is the 5th test doc";
            store.putDocument(makeAnewdoc("somthingrandom5", text5), uri5, DocumentStore.DocumentFormat.TXT);
            //current bytes = 2114, count = 2
            store.setMaxDocumentBytes(1220);//this action should cause stage4uri to be deleted
            store.setMaxDocumentCount(1);
            if (store.getDocumentAsTxt(stage4Uri) == null && store.getDocumentAsTxt(uri5).equals(text5)) {
                System.out.println("Success the docstore can delete and clear up memory appropriately when immeditaly upon setting the max and the current bytes/count is over the max just set");
            } else {
                System.out.println("FAIL!");
            }
            //current Bytes = 890, count = 1
            store.setMaxDocumentBytes(3000);
            store.setMaxDocumentCount(3);
            URI uri6 = new URI("THISISURI6");
            String text6 = "a simple cute 6th document";
            store.putDocument(makeAnewdoc("somthingrandom6", text6), uri6, DocumentStore.DocumentFormat.TXT);
            //2 docs in now uri 5 & uri 6
            //current bytes = 1760, count = 2
            store.deleteDocument(uri6);//count = 1
            //current bytes = 890, as it should.
            //test undo delete that will cause memory overflow
            store.setMaxDocumentBytes(1000);
            store.setMaxDocumentCount(1);
            store.undo();//after undo count = 1, b/c to many docs
            if (store.getDocumentAsTxt(uri6).equals(text6) && store.getDocumentAsTxt(uri4) == null && store.search("5th").size() == 0 && store.search("6th").size() == 1) {
                System.out.println("Success just like the tests preceding this test - but now we know that an undo of a delete (via the deleteDocument) works as is expected when it causes a memory overflow");
            } else {
                System.out.println("Fail");
            }
            int cmdStackSizeBefore = store.commandStack.size();
            store.undo(uri5);
            if (cmdStackSizeBefore == store.commandStack.size()) {
                System.out.println("commands were completely obliterated from the cmd stack relating to the uri that was deleted to free memory");
            } else {
                System.out.println("FAIL");
            }
            //Test time update of a duplicate doc
            //LastUseTime of doc in right now = 5432524494900
            store.putDocument(makeAnewdoc("somethingrandom6", text6), uri6, DocumentStore.DocumentFormat.TXT);
            //LastUseTime now = 5475978734500(TEST WORKED)
            //current count = 1, bytes 878
            store.setMaxDocumentCount(5);
            store.setMaxDocumentBytes(10000);
            URI uri7 = new URI("uricode7beepbopboop");
            URI uri8 = new URI("uricode8beepityboopitybop");
            String text7 = "This is really one of the last test docs for stage 4";
            String text8 = "This is possibly like actually one of if not the last documents";
            store.putDocument(makeAnewdoc("prefixfornumber7", text7), uri7, DocumentStore.DocumentFormat.TXT);
            //current count = 2, bytes 1804
            store.putDocument(makeAnewdoc("thisisaprefixfordoc8", text8), uri8, DocumentStore.DocumentFormat.TXT);
            // current count = 3, bytes 2752
            store.setMaxDocumentBytes(1000);//should delete the old 2 docs and leave text8
            if (store.getDocumentAsTxt(uri8).equals(text8) && store.getDocumentAsTxt(uri7) == null && store.getDocumentAsTxt(uri6) == null) {
                System.out.println("Success the doc store can free up memory by deleting MULTIPLE documents upon being set at a limit that the store is already over");
            } else {
                System.out.println("Failure");
            }
            URI uri9 = new URI("URI9ARGH");
            String text9 = "shortdoc";
            store.setMaxDocumentBytes(3500);//GOING OVER COUNT LIMIT DOESNT REALLY APPLY HERE BECAUSE YOUD NEVER REMOVE MULTIPLE DOCS UPON  GOING OVER A COUNT B/C YOU ONLY ADD ONE DOC AT A TIME
            store.putDocument(makeAnewdoc("prefixforuri9", text9), uri9, DocumentStore.DocumentFormat.TXT);
            URI uri10 = new URI("LASTURIBIG");
            String text10 = "this is a text document with a lot of data in it that should be make it that both docs need to be deleted it put this in ssssssssssssssssssssssssssssssssssssssssssssssssssssssrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr hhhhhh please beeee bigger we need you to be bigger than the other two docs combined why can tyo ujust do that aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa weeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeecmonnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnbhbyi";
            store.setMaxDocumentBytes(2000);
            store.putDocument(makeAnewdoc("prefixforuri10", text10), uri10, DocumentStore.DocumentFormat.TXT);
            if (store.getDocumentAsTxt(uri8) == null && store.getDocumentAsTxt(uri9) == null && store.getDocumentAsTxt(uri10).equals(text10)) {
                System.out.println("SUCCESS! same as last test except this is adding a document that requires multiple documents to be removed where the limit has already been set before");
            } else {
                System.out.println("FAIl");
            }
            store.setMaxDocumentBytes(10000);
            store.setMaxDocumentCount(10);
            String text11 = "anything";
            URI uri11 = new URI("undoNewPutUri");
            store.putDocument(makeAnewdoc("prefixforUri11", text11), uri11, DocumentStore.DocumentFormat.TXT);
            store.undo();
            if (store.getDocumentAsTxt(uri11) == null) {
                System.out.println("Succes undoing a putNewDoc worked");
            } else {
                System.out.println("Failure!");
            }
            String text12 = "PLEASE SHOW UP ON DISK";
            URI uri12 = new URI("http://www.yu.edu/documents/doc1");
            store.putDocument(makeAnewdoc("http://", text12), uri12, DocumentStore.DocumentFormat.TXT);
            Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentSerializer()).setPrettyPrinting().create();//()GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
            String serializedDoc = gson.toJson(store.getDocument(uri12));
            System.out.println(uri12.getPath().replace("/", "\\"));
            String sep = File.separator;
            System.out.println(System.getProperty("user.dir") + sep);
            System.out.println(uri12.getSchemeSpecificPart());
            //File myFile = new File(System.getProperty("user.dir") + uri12.getPath().replace("/", "\\"));
            String uriPath = uri12.getSchemeSpecificPart().replace("/", "\\");//(uri12.getAuthority() + uri12.getPath())
            int index = uriPath.lastIndexOf("\\");
            String dirStruc = uriPath.substring(0,index);
            //System.out.println(uri12.);
            File myFileStruc = new File(System.getProperty("user.dir") + sep + dirStruc);
            System.out.println(System.getProperty("user.dir").getClass());
            System.out.println(myFileStruc.mkdirs());
            File myNewFile = new File(myFileStruc + sep + uriPath.substring(index + 1) + ".json");
            System.out.println(myNewFile.createNewFile());
            //FileWriter tool = new FileWriter(myFile);
            OutputStream os = new FileOutputStream(myNewFile);
            PrintWriter pw = new PrintWriter(os);
            pw.print(serializedDoc);
            pw.close();
            //BufferedReader br = new BufferedReader(new FileReader(myNewFile));
            /*String filepath = dirStruc + File.separator + uriPath.substring(index + 1) + ".json";
            String json = new String(Files.readAllBytes(Paths.get(filepath)));
            System.out.println(json);
            Gson gson1 = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer()).create();
            Files.delete(Paths.get(filepath));
            DocumentImpl deCerealDoc = gson.fromJson(json, DocumentImpl.class);
            //System.out.println((int)null);
            //How to delete the files after base dir that are empty?
            //.close();*/
            System.out.println();
        } catch (URISyntaxException | IOException e) {
            System.out.println("URI ERROR or IO");
        }
    }

    public static InputStream makeAnewdoc(String prefix, String text) {
        try {
            File tempTxt = File.createTempFile(prefix, ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempTxt));
            bw.write(text);
            bw.close();
            return new FileInputStream(tempTxt);
        } catch (IOException e) {
            System.out.println("URI ERROR or IO");
        }
        return null;
    }
}

