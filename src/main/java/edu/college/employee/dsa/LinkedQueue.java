package edu.college.employee.dsa;

import java.util.Objects;

/** Singly linked FIFO queue, used internally for pending leave review and graph BFS. */
public final class LinkedQueue<T> {
    private static final class Node<T> {
        private final T value;
        private Node<T> next;
        private Node(T value) { this.value = value; }
    }

    private Node<T> front;
    private Node<T> rear;
    private int size;

    public int size() { return size; }
    public boolean isEmpty() { return front == null; }

    public void enqueue(T value) {
        Node<T> node = new Node<>(Objects.requireNonNull(value));
        if (rear == null) front = rear = node;
        else { rear.next = node; rear = node; }
        size++;
    }

    public T dequeue() {
        if (front == null) throw new IllegalStateException("Queue is empty");
        T value = front.value;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return value;
    }

    public T peek() {
        if (front == null) throw new IllegalStateException("Queue is empty");
        return front.value;
    }

    public void clear() { front = rear = null; size = 0; }
}
