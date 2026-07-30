'use strict';

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Envolve um cliente HTTP (axios) com fila serial e intervalo mínimo entre requests,
 * evitando rajadas contra os portais governamentais.
 */
function createGuardedHttp(http, { minDelayMs = 200 } = {}) {
  let lastFetchAt = 0;
  let chain = Promise.resolve();

  function schedule(fn) {
    chain = chain.then(async () => {
      const now = Date.now();
      const wait = Math.max(0, minDelayMs - (now - lastFetchAt));
      if (wait > 0) await sleep(wait);
      lastFetchAt = Date.now();
      return fn();
    });
    return chain;
  }

  return {
    get: (url, config) => schedule(() => http.get(url, config)),
    post: (url, data, config) => schedule(() => http.post(url, data, config)),
  };
}

function createRefreshGuard(cooldownMs) {
  const lastAt = { prefeitura: 0, camara: 0, all: 0 };

  function canRefresh(source) {
    const key = source === 'prefeitura' || source === 'camara' ? source : 'all';
    const elapsed = Date.now() - (lastAt[key] || 0);
    return elapsed >= cooldownMs;
  }

  function markRefreshed(source) {
    const now = Date.now();
    if (source === 'prefeitura' || source === 'camara') {
      lastAt[source] = now;
    } else {
      lastAt.all = now;
      lastAt.prefeitura = now;
      lastAt.camara = now;
    }
  }

  function remainingMs(source) {
    const key = source === 'prefeitura' || source === 'camara' ? source : 'all';
    return Math.max(0, cooldownMs - (Date.now() - (lastAt[key] || 0)));
  }

  return { canRefresh, markRefreshed, remainingMs };
}

function createScrapeLock() {
  const inFlight = new Map();

  function run(key, fn) {
    if (inFlight.has(key)) return inFlight.get(key);
    const promise = Promise.resolve()
      .then(fn)
      .finally(() => inFlight.delete(key));
    inFlight.set(key, promise);
    return promise;
  }

  return { run };
}

module.exports = {
  createGuardedHttp,
  createRefreshGuard,
  createScrapeLock,
  sleep,
};
