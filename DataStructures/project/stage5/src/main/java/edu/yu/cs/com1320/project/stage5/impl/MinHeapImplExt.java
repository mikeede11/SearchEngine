package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URI;

class MinHeapImplExt<T extends Comparable> extends MinHeapImpl<T>{
    public MinHeapImplExt(){
        super();
    }
    protected boolean inMinHeap(T element){
        if(super.getArrayIndex(element) != -1){return true;}
        else{return false;}
    }
    protected void setBTree(BTreeImpl<URI, Document> bTree){
        this.bTree = bTree;
        this.bTreeSet = true;
    }
}
