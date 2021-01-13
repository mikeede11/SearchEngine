package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;
import java.util.*;

public class TrieImpl<Value> implements Trie<Value>{
    private Node root = new Node();
    private static final int R = 91;

    private static class Node{
        private Object value;
        private Node[] links = new Node[R];
    }
    //default java constructor

    @Override
    public void put(String key, Value val) {
        if(key == null || val == null){return;}//ASKBOUT EPMTY  STRING
        String processedKey = processKey(key);
        Node x = root;
        for(int i = 0; i < processedKey.length(); i++){
            if(x.links[processedKey.charAt(i)] == null){ x.links[processedKey.charAt(i)] = new Node();}
            x = x.links[processedKey.charAt(i)];
            if(i == processedKey.length() - 1){
                if(x.value != null) {((Set<Value>)x.value).add(val);}//This will add it to the set if its not in it and wont if it is.
                else{
                    x.value = new HashSet<Value>();
                    ((Set<Value>)x.value).add(val);
                }
            }
        }
    }

    private String processKey(String key){
        String upCas = key.toUpperCase();
        upCas = upCas.replaceAll("[^0-9A-Z]", "");
        return upCas;
    }

    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if(key == null || comparator == null){return new ArrayList<Value>();}
        String processedKey = processKey(key);
        Node x = root;
        for(int i = 0; i < processedKey.length(); i++) {
            //x = x.links[processedKey.charAt(i)];
            if (x == null) {
                return (List<Value>) new ArrayList<>();
            }
            x = x.links[processedKey.charAt(i)];
            if (i == processedKey.length() - 1 && x != null) {
                if (x.value != null) {
                    List<Value> sortedList = new ArrayList<Value>((Set<Value>) x.value);
                    Collections.sort(sortedList, comparator);
                    return sortedList;
                }
            }
        }
        return (List<Value>) new ArrayList<>();
    }

    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(prefix == null){return new HashSet<Value>();}
        Set<Value> masterSet = new HashSet<>();
        List<String> keysWPrefix = new ArrayList<>();
        String newPrefix = processKey(prefix);
        Node x = this.get(this.root, newPrefix, 0);//EVERYTHING IS CONTINGENT ON THIS RETURNING NULL IF NODE IS NOT THERE.
        if( x != null){
            this.collect(x, new StringBuilder(newPrefix), keysWPrefix);
            for(String key: keysWPrefix){
                masterSet.addAll(deleteAll(key));
            }
        }
        return masterSet;
    }

    @Override
    public Set<Value> deleteAll(String key) {
        if(key == null){return new HashSet<Value>();}
        Set<Value> delVals = new HashSet<>();
        String newKey = processKey(key);
        boolean removed = false;
        //delVals.addAll((Set<Value>)get(root, newKey, 0).value);
        root = deleteNode(this.root, newKey, 0, delVals);
        //if(get(root, newKey, 0) == null || get(root, newKey, 0).value == null) {removed = true;}
        //if(removed) {return delVals;}
        //else{ return ;}
        return delVals; //dont need to check if it loaded up it will return what it should if not it will return an empty set which it should - if it doesnt thats a problem with delete node and others.
    }

    @Override
    public Value delete(String key, Value val) {// TODO: 4/7/2020
        if(key == null || val == null){return null;}
        String newKey = processKey(key);
        boolean removed = false;
        root = deleteNode(this.root, newKey, 0, val);
        if(get(root, newKey, 0) == null || get(root, newKey, 0).value == null || !((Set<Value>)get(root, newKey, 0).value).contains(val)) {removed = true;}//problem: first you test if the NODE is null, not the value, then b/c thenode is not null you test if it doesnt have the speciifc value anymore - which is faulty b/c what if the node is not null(as in its part of a larger word, but its value is null well then youll get a null pointer exception when you call a set method on a set thats not there.
        if(removed){return val;}//problem fixed using short circuit eval. to my advantage.
        else{return null;}
    }

    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(prefix == null || comparator == null){return new ArrayList<Value>();}
        List<String> allDocsWPrefix = new ArrayList<>();
        Set<Value> docsWPrefixSorted = new HashSet<>();
        String newPrefix = processKey(prefix);
        Node x = this.get(this.root, newPrefix, 0);//this method should process string and return null if a) no node
        if(x != null){//maybe x.value- this says if x is a node,say p then collect all docs at/under should return 2 docs
            this.collect(x, new StringBuilder(newPrefix), allDocsWPrefix);
        }
        for(String key: allDocsWPrefix){
            docsWPrefixSorted.addAll(getAllSorted(key, comparator));
        }
        List<Value> listOfDocs = new ArrayList<Value>(docsWPrefixSorted);
        Collections.sort(listOfDocs, comparator);
        return listOfDocs;
    }

    void collect(Node x, StringBuilder prefix, List<String> results) {
        if (x.value != null) results.add(prefix.toString());
        for (char c = 0; c < TrieImpl.R; c++) {
            if(x.links[c] != null) {
                prefix.append(c);
                this.collect(x.links[c], prefix, results);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }
    }

    private Node get(Node x, String key, int d){
        if(x == null) return null;
        if(d == key.length()) return x;
        else {char c = key.charAt(d); return get(x.links[c], key, d + 1);}
    }

    private <T> Node deleteNode(Node x, String key, int d, T thing ){//brilliant
        if(x == null){
            return null;
        }
        if(d == key.length()){
            if(thing instanceof HashSet){
                ((HashSet) thing).addAll((Collection) x.value);
                x.value = null;
            }
            else if(!(x.value == null)){//make sure the document/value is there(i.e. it wasnt already deleted at this node - this can occur if theres double wording in a doc
                ((Set<Value>)x.value).remove(thing);
                if(((Set<Value>) x.value).isEmpty()){x.value = null;}
            }
            //delLog.accept((Value)x.value);
        }
        else{
            char c = key.charAt(d);
            x.links[c] = deleteNode(x.links[c], key, d +1, thing);
        }
        if(x.value != null || x == root){//two ways two ensure root is not null when it needs to have value. A) set instance variable to new Node() and create exception for deleting the node which is what is done here, or have no value set to IV root and just instantiate it whenever you need in the put method.
            return x;
        }
        for(char c = 0; c < TrieImpl.R; c++){
            if(x.links[c] != null){
                return x;
            }
        }
        return null;
    }
}

