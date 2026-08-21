'use strict';

function parseBrazilianDate(str) {
  if (!str || typeof str !== 'string') return 0;
  const s = str.trim();
  const br = s.match(/(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?/);
  if (br) {
    const d = new Date(+br[3], +br[2] - 1, +br[1], +(br[4] || 0), +(br[5] || 0), +(br[6] || 0));
    const t = d.getTime();
    return Number.isNaN(t) ? 0 : t;
  }
  const iso = Date.parse(s);
  return Number.isNaN(iso) ? 0 : iso;
}

function entityRecency(item) {
  const fromDate = parseBrazilianDate(item.data);
  if (fromDate) return fromDate;
  const ex = parseInt(String(item.exercicio || ''), 10);
  if (ex > 0) return ex * 1e12;
  return 0;
}

function entityKey(item) {
  const id = String(item.id || '').trim();
  if (id) return `id:${id}`;
  const titulo = String(item.titulo || '').trim().toLowerCase().substring(0, 100);
  const ex = String(item.exercicio || item.data || '').trim();
  return titulo ? `t:${titulo}|${ex}` : '';
}

function mergeEntitiesByRecency(lists) {
  const map = new Map();
  for (const list of lists) {
    for (const raw of list || []) {
      if (!raw?.titulo) continue;
      const item = { ...raw };
      const key = entityKey(item);
      if (!key) continue;
      const existing = map.get(key);
      if (!existing || entityRecency(item) >= entityRecency(existing)) {
        map.set(key, item);
      }
    }
  }
  return [...map.values()].sort((a, b) => entityRecency(b) - entityRecency(a));
}

module.exports = {
  entityRecency,
  entityKey,
  mergeEntitiesByRecency,
};
