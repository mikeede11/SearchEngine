package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

public class DocumentImpl implements Document {
    private URI key;
    private String text;
    private int txtHash;
    private byte[] pdfBytes;
    private long lastUseTime;
    private HashMap<String, Integer> wordMap = new HashMap<>();
    public DocumentImpl(URI uri, String txt, int txtHash){
        this.key = uri;
        this.text = txt;
        this.txtHash = txtHash;
        PDDocument pdfDoc = new PDDocument();
        PDPage page = new PDPage();
        pdfDoc.addPage(page);
        InputStream is = null;
        ByteArrayOutputStream out = null;
        try {
            PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
            contentStream.beginText();
            contentStream.newLineAtOffset(25, 700);
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 12);
            contentStream.newLineAtOffset(50, 700);
            contentStream.showText(txt);
            contentStream.endText();
            contentStream.close();
            out = new ByteArrayOutputStream();
            pdfDoc.save(out);
            this.pdfBytes = out.toByteArray();
            pdfDoc.close();
        }catch(IOException e){
            e.printStackTrace();
            System.out.println("The PDF was not created properly");
        }
        for(String word: txt.split(" ")){
            String alphNumWord = word.toUpperCase().replaceAll("[^0-9A-Z]", "");
            if(wordMap.containsKey(alphNumWord)) {wordMap.put(alphNumWord, wordMap.get(alphNumWord) + 1);}
            else{wordMap.put(alphNumWord, 1);}
        }
    }
    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes){
        this(uri, txt, txtHash);
        this.pdfBytes = pdfBytes;
    }
    public byte[] getDocumentAsPdf() {
       // if(pdfBytes == null){
        return this.pdfBytes;
    }

    public String getDocumentAsTxt() {
        return text;
    }

    public int getDocumentTextHashCode() {
        return txtHash;
    }

    public URI getKey() {
        return key;
    }

    public HashMap<String, Integer> getWordMap(){
        return wordMap;
    }
    public void setWordMap(HashMap<String, Integer> wordMap){
        this.wordMap = wordMap;
    }

    @Override
    public int wordCount(String word) {
        String alphNumWord = word.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if(wordMap.containsKey(alphNumWord)) {
            return wordMap.get(alphNumWord);
        }
        else return 0;
    }

    @Override
    public long getLastUseTime() {
        return this.lastUseTime;
    }

    @Override
    public void setLastUseTime(long timeInMilliseconds) {
        this.lastUseTime = timeInMilliseconds;
    }

    @Override
    public int compareTo(Document that){
        /*if(this.lastUseTime == Long.MIN_VALUE){return -1;}
        if(that.getLastUseTime() == Long.MIN_VALUE){return  1;}
        long num = this.lastUseTime - that.getLastUseTime();
        if(num<0){return -1;}
        else if(num > 0){return 1;}
        else{return 0;}*/
        if(this.lastUseTime < that.getLastUseTime()){return -1;}
        else if(this.lastUseTime > that.getLastUseTime()){return 1;}
        else {return 0;}

    }
}