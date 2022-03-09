package Sim.Mobility;

import Sim.Message;
import Sim.Messages.IPv6Tunneled;
import Sim.Messages.MobileIPv6.BindingUpdate;
import Sim.NetworkAddr;
import Sim.Router;

import java.util.HashMap;

public class HomeAgent extends Router {
    //    LRUCache<NetworkAddr, NetworkAddr> _bindingCache = new LRUCache<>(100);
    private HashMap<Long, NetworkAddr> _bindingCache = new HashMap<>();

    public HomeAgent(String name, int interfaces, long basePrefix) {
        super(name, interfaces, basePrefix);
    }

    @Override
    protected boolean isHomeAgent() {
        return true;
    }

    @Override
    protected void processBindingUpdate(BindingUpdate ev) {
        var homeAddress = ev.destination();
        var careOfAddress = ev.source();

        System.out.printf("== %s update binding from %s to %s%n", this, homeAddress, careOfAddress);
        _bindingCache.put(homeAddress.networkId(), careOfAddress);
    }

    @Override
    protected void processTunneledMessage(IPv6Tunneled ev) {
        // If we received a tunneled message where the source is a mobile agent we care for, then we should unwrap it
        // and forward the original message.
        boolean caresFor = false;
        for (var entry : _bindingCache.entrySet()) {
            if (entry.getValue() == ev.source()) {
                caresFor = true;
                break;
            }
        }

        if (caresFor) {
            var original = ev.getOriginalPacket();
            System.out.printf("%s received tunneled message %d from %s which it cares for. Unpacking and forwarding to %s", this, ev.seq(), ev.source(), original.destination());
            forwardMessage(original);
        } else {
            System.out.printf("%s received tunneled message %d. Do nothing and pass along to %s", this, ev.seq(), ev.destination());
            forwardMessage(ev);
        }
    }

    @Override
    protected void forwardMessage(Message ev) {
        if (_bindingCache.containsKey(ev.destination().networkId())) {
            var coa = _bindingCache.get(ev.destination().networkId());
            System.out.printf("== %s tunnels message from (%s -> %s) to (%s -> %s)%n", this, ev.source(), ev.destination(), ev.destination(), coa);
            ev = new IPv6Tunneled(ev.destination(), coa, 0, ev);
        }
        super.forwardMessage(ev);
    }

    /**
     * LRU Cache to store mappings.
     *
     * @param <K> key type.
     * @param <V> value type.
     */
    protected static class LRUCache<K, V> {
        /**
         * Internal node with pointers to previous and next nodes.
         *
         * @param <K> key type.
         * @param <V> value type.
         */
        protected static class Node<K, V> {
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
         * Instantiate a new LRU cache.
         *
         * @param capacity how many entries to store.
         */
        protected LRUCache(int capacity) {
            _capacity = capacity;
            _head = new Node<>(null, null, null, null);
            _tail = new Node<>(null, null, null, null);

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
                    Node<K, V> d = _head._next;
                    clearFromList(d);
                    _cache.remove(d._key);
                }

                Node<K, V> n = new Node<>(null, null, key, value);
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

        private void clearFromList(Node<K, V> n) {
            n._prev._next = n._next;
            n._next._prev = n._prev;
        }

        private void placeBefore(Node<K, V> n, Node<K, V> where) {
            n._next = where;
            n._prev = where._prev;

            where._prev._next = n;
            where._prev = n;
        }
    }
}
