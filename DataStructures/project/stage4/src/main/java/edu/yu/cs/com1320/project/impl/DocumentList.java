package edu.yu.cs.com1320.project.impl;

class DocumentList<Key, Value> {
    private Node firstNode;

    class Node {
        Key key;
        Value value;
        Node nextNode;

        public Node(Key key, Value value, Node next) {
            this.key = key;
            this.value = value;
            this.nextNode = next;
        }

        public Key getKey() {//can this be public
            return key;
        }

        public Value getValue() {
            return value;
        }

        public Node getNextNode(){
            return nextNode;
        }
    }

    Value get(Key key) {
        for (Node x = firstNode; x != null; x = x.nextNode) {
            if (key.equals(x.key)) {
                return x.value;
            }
        }
        return null;
    }

    Node getFirstNode(){
        return firstNode;
    }

    Value put(Key key, Value value) {
        for (Node x = firstNode; x != null; x = x.nextNode) {//this part MODIFIES an existing doc/value(if it finds a matching key)
            if (key.equals(x.key)) {
                Value temp = x.value;
                x.value = value;
                return temp;
            }
        }
        firstNode = new Node(key, value, firstNode);//this part adds completely new doc
        return null;//will notify that we added a completely new document.
        /*I think the way this has been working is when we have a new doc to add what hapens is we make a new document and the point
        it to the first document and then re assign the new document as the first document so now we have
        variable first doc --> new doc --> firstDoc(is nextNode to new doc)
        variable first doc --> 2nd new doc --> new doc --> firstDoc
        first case scenario works because when we pass firstNode as the NextNode arg, firstNode is null! perf example of
        importance of operator precedence.
         */
    }
    boolean removeNode(Key key) {
        Node previousNode = firstNode;
        if (firstNode == null) {return false;}
        if(key.equals(firstNode.key)){
            Node temp = firstNode;
            firstNode = firstNode.nextNode;
            temp = null;
            return true;
        }
        else {
            for (Node currentNode = firstNode.nextNode; currentNode != null; currentNode = currentNode.nextNode) {
                if (key.equals(currentNode.key)) {
                    previousNode.nextNode = currentNode.nextNode;
                    currentNode = null;
                    return true;
                }
                else { previousNode = previousNode.nextNode;}
            }
        }
        return false;
    }
}

