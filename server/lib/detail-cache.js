'use strict';

function createDetailCache({ maxSize = 200, ttlMs = 600_000 } = {}) {
  const store = new Map();

  function key(entity, id) {
    return `${entity}:${id}`;
  }

  function get(entity, id) {
    const k = key(entity, id);
    const entry = store.get(k);
    if (!entry) return null;
    if (Date.now() - entry.at > ttlMs) {
      store.delete(k);
      return null;
    }
    return entry.data;
  }

  function set(entity, id, data) {
    const k = key(entity, id);
    if (store.size >= maxSize) {
      const first = store.keys().next().value;
      store.delete(first);
    }
    store.set(k, { at: Date.now(), data });
  }

  return { get, set };
}

module.exports = { createDetailCache };
