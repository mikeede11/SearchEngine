package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

public class HashTableImpl<Key,Value> implements HashTable<Key, Value>
{
    private DocumentList<Key,Value>[] hashTable;
    private final int LOAD_FACTOR = 4;
    private int numOfNodes;
    public HashTableImpl(){
        hashTable = (DocumentList<Key, Value>[]) new DocumentList[1];//THINK ABOUT MAKING OBJECT TYPE(DOCLIST)
        // for(int i = 0; i < length; i++){
        hashTable[0] = new DocumentList<>();
        //}
    }
    //(DocumentList<Key, Value>[]) new DocumentList[length]
    public Value get(Key k) {
        if (k == null) {throw new IllegalArgumentException();}
        return hashTable[hashFunction(k)].get(k);
    }//if not present DocumentLists get method will return null

    public Value put(Key k, Value v) {
        if(k == null){throw new IllegalArgumentException();}
        if(v == null){//delete entry
            Value tempRef = hashTable[hashFunction(k)].get(k);
            if(hashTable[hashFunction(k)].removeNode(k)){numOfNodes--;}//if a doc was removed the removeNode method will return a boolean and well know wether to subtract from numOfNodes or not
            return tempRef;
        }
        Value doc = hashTable[hashFunction(k)].get(k);
        hashTable[hashFunction(k)].put(k, v);//puts in Linked List
        if(doc == null) {numOfNodes++;}
        if(numOfNodes/hashTable.length >= LOAD_FACTOR){
            reHash();
        }
        return doc;
    }
    private int hashFunction(Key key){
        if (key == null) {throw new IllegalArgumentException();}
        return (key.hashCode() & 0x7fffffff) % hashTable.length;
        // return Math.abs(key.hashCode()%length);
    }//their hashcode() method generates neg. nums

    private void reHash(){
        DocumentList<Key, Value>[] tempRef = hashTable;
        arrayLengthDoubler();//makes hashTable refer to a new hashTable, empty but double the size
        numOfNodes = 0;
        for(DocumentList<Key, Value> bucket: tempRef){
            for(DocumentList<Key, Value>.Node x = bucket.getFirstNode(); x != null; x = x.nextNode){
                this.put(x.getKey(), x.getValue());
            }
        }
    }

    private void arrayLengthDoubler(){
        hashTable = (DocumentList<Key, Value>[]) new DocumentList[hashTable.length * 2];
        for(int i = 0; i < hashTable.length; i++) {
            hashTable[i] = new DocumentList<>();
        }
    }


}
