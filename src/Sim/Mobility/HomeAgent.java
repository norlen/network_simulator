package Sim.Mobility;

import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.NetworkAddr;
import Sim.Router;

import java.util.HashMap;

public class HomeAgent extends Router {
    LRUCache<NetworkAddr, NetworkAddr> _bindingCache = new LRUCache<>(100);

    HomeAgent(int interfaces) {
        super(interfaces);
    }

    @Override
    protected boolean isHomeAgent() {
        return true;
    }

    @Override
    protected void processBindingUpdate(BindingUpdate ev) {
        var homeAddress = ev.destination();
        var careOfAddress = ev.source();
        _bindingCache.put(homeAddress, careOfAddress);
    }

    /**
     * LRU Cache to store mappings.
     *
     * @param <K> key type.
     * @param <V> value type.
     */
    protected class LRUCache<K, V> {
        /**
         * Internal node with pointers to previous and next nodes.
         *
         * @param <K> key type.
         * @param <V> value type.
         */
        protected class Node<K, V> {
            // Reference to next node in list.
            Node<K, V> _next;

            // Reference to previous node in list.
            Node<K, V> _prev;

            // Keep reference to key, so we can check against it later.
            K _key;

            // Actual value we store.
            V _value;

            /**
             * Instantiate a cached node.
             *
             * @param prev  reference to previous node.
             * @param next  reference to next node.
             * @param key   key to cache value.
             * @param value value of the node.
             */
            protected Node(Node<K, V> prev, Node<K, V> next, K key, V value) {
                _prev = prev;
                _next = next;
                _key = key;
                _value = value;
            }
        }

        // Store the cache values.
        HashMap<K, Node<K, V>> _cache = new HashMap<>();

        // Least recently used item, head of the list.
        Node<K, V> _head;

        // Most recently used item, tail of the list.
        Node<K, V> _tail;

        // Maximum capacity of the cache.
        private final int _capacity;

        /**
         * Instantiate
         *
         * @param capacity
         */
        protected LRUCache(int capacity) {
            _capacity = capacity;
            _head = new Node(null, null, null, null);
            _tail = new Node(null, null, null, null);

            _head._next = _tail;
            _tail._prev = _head;
        }

        /**
         * @param key
         * @param value
         */
        void put(K key, V value) {
            if (_cache.containsKey(key)) {
                var n = _cache.get(key);
                n._value = value;

                clearFromList(n);
                placeBefore(n, _tail);
            } else {
                if (_cache.size() == _capacity) {
                    Node d = _head._next;
                    clearFromList(d);
                    _cache.remove(d._key);
                }

                Node n = new Node(null, null, key, value);
                placeBefore(n, _tail);

                _cache.put(key, n);
            }
        }

        /**
         * @param key
         * @return
         */
        V get(K key) {
            var n = _cache.get(key);
            if (n == null) {
                return null;
            }

            clearFromList(n);
            placeBefore(n, _tail);
            return n._value;
        }

        private void clearFromList(Node n) {
            n._prev._next = n._next;
            n._next._prev = n._prev;
        }

        private void placeBefore(Node n, Node where) {
            n._next = where;
            n._prev = where._prev;

            where._prev._next = n;
            where._prev = n;
        }
    }
}
