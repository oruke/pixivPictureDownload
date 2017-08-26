package com.oruke.util

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class SpiderQueue<T>() : ReentrantReadWriteLock() {
    private val queue = LinkedList<T>()
    private val call = LinkedList<T>()
    private val lock = ReentrantLock()

    constructor(t: T) : this() {
        queue.add(t)
    }

    fun size(): Int {
        readLock().lock()
        val size = queue.size
        readLock().unlock()
        return size
    }

    fun poll(): T? {
        writeLock().lock()
        val t = queue.poll()
        call.offer(t)
        writeLock().unlock()
        return t
    }

    fun contains(element: T): Boolean {
        readLock().lock()
        val b = queue.contains(element) || call.contains(element)
        readLock().unlock()
        return b
    }

    fun clear() {
        writeLock().lock()
        queue.clear()
        call.clear()
        writeLock().unlock()
    }

    fun remove(element: T): Boolean {
        writeLock().lock()
        queue.remove(element)
        writeLock().unlock()
        return true
    }

    fun element(): T {
        readLock().lock()
        val t = queue.element()
        readLock().unlock()
        return t
    }

    fun removeAll(elements: Collection<T>): Boolean {
        writeLock().lock()
        queue.removeAll(elements)
        writeLock().unlock()
        return true
    }

    fun add(element: T): Boolean {
        writeLock().lock()
        if (!contains(element)) queue.add(element)
        writeLock().unlock()
        return true
    }

    fun isEmpty(): Boolean {
        readLock().lock()
        val b = queue.isEmpty()
        readLock().unlock()
        return b
    }

    fun addAll(elements: Collection<T>): Boolean {
        writeLock().lock()
        elements.forEach { if (!contains(it)) queue.add(it) }
        writeLock().unlock()
        return true
    }

}