package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import edu.yu.cs.com1320.project.stage5.impl.DocumentPersistenceManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key , Value> {//?
    private static final int MAX = 6;//TODO
    private int height;
    private int n;
    private Node root;
    private DocumentPersistenceManager dpm;

    public BTreeImpl(){
        this.root = new Node(0);
        this.dpm = new DocumentPersistenceManager(new File(System.getProperty("user.dir")));
    }

    private static final class Node {
        private int entryCount;
        private Entry[] entries = new Entry[BTreeImpl.MAX];
        private Node next;
        private Node previous;

        // create a node with k entries
        private Node(int k) {
            this.entryCount = k;
        }

        private void setNext(Node next) {
            this.next = next;
        }

        private Node getNext() {
            return this.next;
        }

        private void setPrevious(Node previous) {
            this.previous = previous;
        }

        private Node getPrevious() {
            return this.previous;
        }

        private Entry[] getEntries() {
            return Arrays.copyOf(this.entries, this.entryCount);
        }
    }

    protected static class Entry {//you changed static
        private Comparable key;
        private Object val;
        private Node child;

        public Entry(Comparable key, Object val, Node child) {
            this.key = key;
            this.val = val;
            this.child = child;
        }

        public Object getValue() {
            return this.val;
        }

        public Comparable getKey() {
            return this.key;
        }
    }
    @Override
    public Value get(Key key) {
        if (key == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }
        Entry entry = this.get(this.root, key, this.height);
        if(entry != null) {
            //if entry.val is null either A) Doc has been written to disk or B)it has been deleted
            if (entry.val == null) {
                return (Value) getDocFromDiskAndPutBackInBTree(entry);
            } else {
                return (Value) entry.val;
            }
        }
        //didnt find the key
        return null;
    }

    private Entry get(Node currentNode, Key key, int height) {
        Entry[] entries = currentNode.entries;
        //current node is external (i.e. height == 0)
        if (height == 0) {
            for (int j = 0; j < currentNode.entryCount; j++) {
                if(isEqual(key, entries[j].key)){
                    //found desired key. Return its value
                    return entries[j];
                }
            }
            //didn't find the key
            return null;
        }
        //current node is internal (height > 0)
        else {
            for (int j = 0; j < currentNode.entryCount; j++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be in the subtree below the current entry),
                //then recurse into the current entry’s child
                if (j + 1 == currentNode.entryCount || less(key, entries[j + 1].key)) {
                    return this.get(entries[j].child, key, height - 1);
                }
            }
            //didn't find the key
            return null;
        }
    }
    private Document getDocFromDiskAndPutBackInBTree(Entry e){
        Document doc = null;
        try{
            doc = dpm.deserialize((URI)e.key);
            if(doc != null) {//it might have been deleted
                //calcBeforePut(doc);//ensures mem. specifications are met
                //doc.setLastUseTime(System.nanoTime());//Maybe keep
                e.val = (Value) doc;
            }
        }catch(IOException except) {
            except.printStackTrace();
        }
        return doc;
    }

    private static boolean isEqual(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) == 0;
    }



    @Override
    public Value put(Key key, Value val) {
        if (key == null) {
            throw new IllegalArgumentException("argument key to put() is null");
        }
        //if the key already exists in the b-tree, simply replace the value
        Entry alreadyThere = this.get(this.root, key, this.height);
        //if its not on disk than this will just set the value to null(deleting it)
        if(alreadyThere != null) {
            if(alreadyThere.val == null){
                Value valOffDisk = deserialize(key);//if your modifying it you need to get old version off disk and delete it
                //if(valOffDisk == null){return null;}//if it wasnt in the BTREE oron disk it was deleted return null
                alreadyThere.val = val;
                if(valOffDisk == null){return null;}//if it wasnt in the BTREE oron disk it was deleted return null
                return valOffDisk; //this was the oldValue you replaced
            }
            else {
                Value tempVal = (Value) alreadyThere.val;
                alreadyThere.val = val;
                return tempVal;
            }
        }
        //otherwise we are putting a new value and therefore filling an additional spot.
        Node newNode = this.put(this.root, key, val, this.height);
        this.n++;//add # of key val pairs in btree
        if (newNode == null)
        {
            //we didn't need to create a new node and we are returning null because we filled a previously vacant spot in an already existing node
            return null;
        }

        //if code hasn't returned yet it means a new node was created to accomodate new value - split the root:
        //Create a new node to be the root.
        //Set the old root to be new root's first entry.
        //Set the node returned from the call to put to be new root's second entry
        Node newRoot = new Node(2);
        newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
        this.root = newRoot;
        //a split at the root always increases the tree height by 1
        this.height++;
        return null;//we added a new entry - this wasnt a replacement - thats why the tree got bigger.
    }

    private Value deserialize(Key key){
        Document doc = null;
        try{
            doc = dpm.deserialize((URI) key);
        }catch (NoSuchFileException e){
            return null;
        }catch (IOException d){
            d.printStackTrace();
        }
        return (Value) doc;
    }

    /**
     *
     * @param currentNode
     * @param key
     * @param val
     * @param height
     * @return null if no new node was created (i.e. just added a new Entry into an existing node). If a new node was created due to the need to split, returns the new node
     */
    private Node put(Node currentNode, Key key, Value val, int height) {
        int j;//since alot of loops we just make one variable.
        Entry newEntry = new Entry(key, val, null);
        //external node
        if (height == 0) {
            //find index in currentNode’s entry[] to insert new entry
            //we look for key < entry.key since we want to leave j
            //pointing to the slot to insert the new entry, hence we want to find
            //the first entry in the current node that key is LESS THAN
            for (j = 0; j < currentNode.entryCount; j++) {
                if (less(key, currentNode.entries[j].key)) {
                    break;//now go down to logic that will put value in(move over vals)
                }
            }
        }
        // internal node
        else {
            //find index in node entry array to insert the new entry
            for (j = 0; j < currentNode.entryCount; j++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be added to the subtree below the current entry),
                //then do a recursive call to put on the current entry’s child
                if ((j + 1 == currentNode.entryCount) || less(key, currentNode.entries[j + 1].key)) {
                    //increment j (j++) after the call so that a new entry created by a split
                    //will be inserted in the next slot
                    Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1);
                    if (newNode == null) {
                        return null;
                    }
                    //if the call to put returned a node, it means I need to add a new entry to
                    //the current node
                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        //shift entries over one place to make room for new entry
        for (int i = currentNode.entryCount; i > j; i--)
        {
            currentNode.entries[i] = currentNode.entries[i - 1];//just generally takes all vals after val slot you want to put in and moves them down one
        }
        //this might fill node up, but thats ok b/c we always split it after(thats why there is always 1 slot available temporarily.
        //add new entry
        currentNode.entries[j] = newEntry;//which is now available b/c everything was moved down from this point.
        currentNode.entryCount++;
        if (currentNode.entryCount < BTreeImpl.MAX)
        {
            //no structural changes needed in the tree
            //so just return null
            return null;
        }
        else
            {
            //will have to create new entry in the parent due
            //to the split, so return the new node, which is
            //the node for which the new entry will be created
            return this.split(currentNode, height);
        }
    }

    // comparison functions - make Comparable instead of Key to avoid casts
    private static boolean less(Comparable k1, Comparable k2) {
        return k1.compareTo(k2) < 0;
    }

    private Node split(Node currentNode, int height) {
        Node newNode = new Node(BTreeImpl.MAX / 2);
        //by changing currentNode.entryCount, we will treat any value
        //at index higher than the new currentNode.entryCount as if
        // it doesn't exist
        currentNode.entryCount = BTreeImpl.MAX / 2;//The keys are still in the leftmost branch/left branch of every split, but it could be that everytime we search it ignores all those extra entries
        //copy top half of h into t
        for (int j = 0; j < BTreeImpl.MAX / 2; j++) {
            newNode.entries[j] = currentNode.entries[BTreeImpl.MAX / 2 + j];//second half of curretn node goes into newNode
        }
        //external node
        if (height == 0) {
            newNode.setNext(currentNode.getNext());
            newNode.setPrevious(currentNode);
            currentNode.setNext(newNode);
        }
        return newNode;
    }


    @Override
    public void moveToDisk(Key k) throws Exception {
        Document doc = (Document)put(k, null);
        dpm.serialize((URI) k, doc);
    }

    @Override
    public void setPersistenceManager(PersistenceManager pm) {
        this.dpm = (DocumentPersistenceManager) pm;
    }
}
