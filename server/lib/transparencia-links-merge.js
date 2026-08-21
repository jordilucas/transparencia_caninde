'use strict';

const { HUB_URL } = require('./scraper-acesso-informacao');
const { SST_PORTAL_URL } = require('./portal-urls');

function normalizeUrlKey(url) {
  return String(url || '').trim().toLowerCase().replace(/\/$/, '').replace(/\?clean=false/, '');
}

function linkPriority(link) {
  let score = 0;
  if (link.fonteOrigem === 'hub') score += 20;
  if (link.secao) score += 5;
  if (/governotransparente|sstransparencia|acessoainformacao|dadosabertos/.test(link.url || '')) score += 3;
  return score;
}

function mergeTransparenciaLinks(...groups) {
  const map = new Map();

  for (const list of groups) {
    for (const raw of list || []) {
      if (!raw?.url) continue;
      const link = {
        titulo: String(raw.titulo || '').trim(),
        url: String(raw.url || '').trim(),
        categoria: String(raw.categoria || 'portal').trim() || 'portal',
        secao: String(raw.secao || '').trim(),
      };
      if (!link.titulo || !link.url) continue;

      const key = normalizeUrlKey(link.url);
      const existing = map.get(key);
      if (!existing || linkPriority(raw) > linkPriority(existing)) {
        map.set(key, link);
      } else if (existing && !existing.secao && link.secao) {
        map.set(key, { ...existing, secao: link.secao });
      }
    }
  }

  const hubEntry = {
    titulo: 'Acesso à Informação — hub oficial',
    url: HUB_URL,
    categoria: 'portal',
    secao: 'Portal da transparência',
  };
  map.set(normalizeUrlKey(HUB_URL), hubEntry);

  const sstEntry = {
    titulo: 'Portal S&S — transparência orçamentária',
    url: SST_PORTAL_URL,
    categoria: 'financeiro',
    secao: 'Execução orçamentária',
  };
  map.set(normalizeUrlKey(SST_PORTAL_URL), sstEntry);

  return [...map.values()].sort((a, b) => {
    const sa = a.secao || a.categoria;
    const sb = b.secao || b.categoria;
    const sec = sa.localeCompare(sb, 'pt-BR');
    if (sec !== 0) return sec;
    return a.titulo.localeCompare(b.titulo, 'pt-BR');
  });
}

module.exports = {
  mergeTransparenciaLinks,
  normalizeUrlKey,
};
