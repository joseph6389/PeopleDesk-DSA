package edu.college.employee.dsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** A genuine singly linked list with no collection-backed storage. */
public final class SinglyLinkedList<T> {
    private static final class Node<T> { T value; Node<T> next; Node(T v) { value=v; } }
    private Node<T> head, tail; private int size;
    public int size(){return size;} public boolean isEmpty(){return size==0;}
    public void insert(T value){Node<T> n=new Node<>(Objects.requireNonNull(value)); if(tail==null) head=tail=n; else {tail.next=n;tail=n;} size++;}
    public T search(Predicate<T> predicate){for(Node<T> n=head;n!=null;n=n.next)if(predicate.test(n.value))return n.value;return null;}
    public boolean delete(Predicate<T> predicate){Node<T> prev=null,n=head;while(n!=null){if(predicate.test(n.value)){if(prev==null)head=n.next;else prev.next=n.next;if(tail==n)tail=prev;size--;return true;}prev=n;n=n.next;}return false;}
    public boolean update(Predicate<T> predicate,T replacement){Objects.requireNonNull(replacement);for(Node<T> n=head;n!=null;n=n.next)if(predicate.test(n.value)){n.value=replacement;return true;}return false;}
    public List<T> traverse(){List<T> out=new ArrayList<>(size);for(Node<T> n=head;n!=null;n=n.next)out.add(n.value);return List.copyOf(out);}
    /** Removes every node and releases both ends of the chain. */
    public void clear(){head=null;tail=null;size=0;}
}
