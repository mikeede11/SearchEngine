package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {
    private StackNode topNode;
    private int stackElements;

    //JAVA DEFAULT CONSTRUCTOR

    private class StackNode {
        T element;
        StackNode nextNode;

        public StackNode(T element, StackNode next) {
            this.element = element;
            this.nextNode = next;
        }

        public StackNode getNextNode(){
            return nextNode;
        }
    }

    @Override
    public void push(T element) {
        topNode = new StackNode(element, topNode);//I think this covers scenario 1) empty stack so it would create a new node with topNode which is null as nextNode and then set topNode to this Node which is exactly what we want
        //case 2) there is something in the stack in which case we create a new node and then make its nextNode the firstNode and then reassign topNode to the new Node i.e. we added to topOfStack
        //problem that might occur is null as argument easy fix.
        stackElements++;
    }

    @Override
    public T pop() {
        if(stackElements == 0){ return null;}
        T tempRef = topNode.element;
        topNode = topNode.nextNode;//this takes the reference to the nextNode and assigns it to our top node so A) our orig. topOfSTack is gone (GC - no ref) B) topOfStack is now node under it.
        stackElements--;
        return tempRef;
    }

    @Override
    public T peek() {
        if(stackElements == 0){ return null;}
        return this.topNode.element;
    }

    @Override
    public int size() {
        return stackElements;
    }
}

