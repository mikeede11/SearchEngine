package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.Arrays;
// TODO: 4/19/2020 extend? 

public class MinHeapImpl<T extends Comparable> extends MinHeap<T>{

    public MinHeapImpl(){
        super();
    }

    @Override
    public void reHeapify(T element) {
        super.upHeap(getArrayIndex(element));
        super.downHeap(getArrayIndex(element));
    }

    @Override
    protected int getArrayIndex(T element) {
        return (int)super.elementsToArrayIndex.get(element);//no auto-unboxing?
    }

    @Override
    protected void doubleArraySize() {
        super.elements = Arrays.copyOf(super.elements, super.elements.length * 2);
    }
    
}
