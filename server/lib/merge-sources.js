'use strict';

const { filterValidLicitacoes } = require('./licitacao-html');

function parseBrazilianDate(str) {
  if (!str || typeof str !== 'string') return 0;
  const s = str.trim();
  const br = s.match(/(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?/);
  if (br) {
    const d = new Date(
      +br[3],
      +br[2] - 1,
      +br[1],
      +(br[4] || 0),
      +(br[5] || 0),
      +(br[6] || 0),
    );
    const t = d.getTime();
    return Number.isNaN(t) ? 0 : t;
  }
  const iso = Date.parse(s);
  return Number.isNaN(iso) ? 0 : iso;
}

function fieldScore(obj, fields) {
  if (!obj) return 0;
  return fields.reduce((n, f) => n + (String(obj[f] || '').trim() ? 1 : 0), 0);
}

function pickNewer(a, b, recencyFn) {
  const ra = recencyFn(a);
  const rb = recencyFn(b);
  if (rb > ra) return { winner: b, loser: a };
  if (ra > rb) return { winner: a, loser: b };
  return null;
}

function mergeObjects(preferred, fallback) {
  if (!fallback) return { ...preferred };
  if (!preferred) return { ...fallback };
  const out = { ...fallback, ...preferred };
  for (const key of Object.keys(fallback)) {
    const pv = preferred[key];
    const fv = fallback[key];
    if (pv == null || pv === '') out[key] = fv;
    else if (typeof pv === 'object' && pv !== null && !Array.isArray(pv) && typeof fv === 'object' && fv !== null) {
      out[key] = mergeObjects(pv, fv);
    }
  }
  return out;
}

function mergeEntityLists(listA, listB, { getKey, getRecency, mergePair, tieBreak }) {
  const map = new Map();
  const sources = new Set();

  for (const raw of [...(listA || []), ...(listB || [])]) {
    if (!raw) continue;
    const item = { ...raw };
    if (item.fonteOrigem) sources.add(item.fonteOrigem);
    const key = getKey(item);
    if (!key) continue;

    const existing = map.get(key);
    if (!existing) {
      map.set(key, item);
      continue;
    }

    const picked = pickNewer(existing, item, getRecency);
    if (picked) {
      map.set(key, mergePair(picked.winner, picked.loser));
    } else {
      const tie = tieBreak ? tieBreak(existing, item) : item;
      const other = tie === item ? existing : item;
      map.set(key, mergePair(tie, other));
    }
  }

  const merged = Array.from(map.values());
  merged.sort((a, b) => getRecency(b) - getRecency(a));
  return { items: merged, sourcesUsed: [...sources] };
}

function contratoKey(c) {
  const n = String(c.numero || '').trim().toLowerCase();
  if (n) return `n:${n}`;
  const id = String(c.id || '').trim();
  return id ? `id:${id}` : '';
}

function contratoRecency(c) {
  return parseBrazilianDate(c.data);
}

function mergeContratoPair(winner, loser) {
  const merged = mergeObjects(winner, loser);
  merged.fonteOrigem = winner.fonteOrigem || loser.fonteOrigem || '';
  return merged;
}

function mergeContratos(jsonList, htmlList) {
  return mergeEntityLists(jsonList, htmlList, {
    getKey: contratoKey,
    getRecency: contratoRecency,
    mergePair: mergeContratoPair,
    tieBreak: (a, b) => (fieldScore(a, ['pdfUrl', 'cnpjCredor', 'secretaria', 'valor']) >= fieldScore(b, ['pdfUrl', 'cnpjCredor', 'secretaria', 'valor']) ? a : b),
  });
}

function licitacaoKey(l) {
  const n = String(l.numero || '').trim().toLowerCase();
  if (n) return `n:${n}`;
  const id = String(l.id || '').trim();
  return id ? `id:${id}` : '';
}

function licitacaoRecency(l) {
  return parseBrazilianDate(l.dataAbertura);
}

function mergeLicitacaoPair(winner, loser) {
  const merged = mergeObjects(winner, loser);
  merged.fonteOrigem = winner.fonteOrigem || loser.fonteOrigem || '';
  return merged;
}

function mergeLicitacoes(jsonList, htmlList) {
  return mergeEntityLists(jsonList, htmlList, {
    getKey: licitacaoKey,
    getRecency: licitacaoRecency,
    mergePair: mergeLicitacaoPair,
    tieBreak: (a, b) => (fieldScore(a, ['url', 'modalidade', 'objeto']) >= fieldScore(b, ['url', 'modalidade', 'objeto']) ? a : b),
  });
}

function secretariaKey(s) {
  const id = String(s.id || '').trim();
  if (id) return `id:${id}`;
  const nome = String(s.nome || '').trim().toLowerCase();
  return nome ? `nome:${nome}` : '';
}

function secretariaRecency(s) {
  const contactScore = fieldScore(s.contato || {}, ['email', 'telefone', 'horarioFuncionamento']);
  const hasGestor = s.secretario ? 1 : 0;
  const jsonBonus = s.fonteOrigem === 'json' ? 1 : 0;
  return contactScore * 1e6 + hasGestor * 1e5 + jsonBonus;
}

function mergeSecretariaPair(winner, loser) {
  const merged = mergeObjects(winner, loser);
  if (winner.contato || loser.contato) {
    merged.contato = mergeObjects(winner.contato || {}, loser.contato || {});
  }
  merged.fonteOrigem = winner.fonteOrigem || loser.fonteOrigem || '';
  return merged;
}

function mergeSecretarias(jsonList, htmlList) {
  return mergeEntityLists(jsonList, htmlList, {
    getKey: secretariaKey,
    getRecency: secretariaRecency,
    mergePair: mergeSecretariaPair,
    tieBreak: (a, b) => (secretariaRecency(a) >= secretariaRecency(b) ? a : b),
  });
}

function gestorKey(g) {
  const nome = String(g.nome || '').trim().toLowerCase();
  return nome ? `nome:${nome}` : '';
}

function gestorRecency(g) {
  const cargoScore = /^prefeito/i.test(String(g.cargo || '')) ? 2 : (/vice/i.test(String(g.cargo || '')) ? 1 : 0);
  const fotoScore = g.foto ? 1 : 0;
  return cargoScore * 10 + fotoScore;
}

function mergeGestorPair(winner, loser) {
  const merged = mergeObjects(winner, loser);
  merged.fonteOrigem = winner.fonteOrigem || loser.fonteOrigem || '';
  return merged;
}

function mergeGestores(jsonList, htmlList) {
  return mergeEntityLists(jsonList, htmlList, {
    getKey: gestorKey,
    getRecency: gestorRecency,
    mergePair: mergeGestorPair,
    tieBreak: (a, b) => (gestorRecency(a) >= gestorRecency(b) ? a : b),
  });
}

function publicacaoKey(p) {
  const id = String(p.id || '').trim();
  if (id) return `id:${id}`;
  const titulo = String(p.titulo || '').trim().toLowerCase().substring(0, 80);
  const data = String(p.data || '').trim();
  return titulo ? `t:${titulo}|${data}` : '';
}

function publicacaoRecency(p) {
  return parseBrazilianDate(p.data);
}

function mergePublicacaoPair(winner, loser) {
  const merged = mergeObjects(winner, loser);
  merged.fonteOrigem = winner.fonteOrigem || loser.fonteOrigem || '';
  return merged;
}

function diarioTextoToPublicacao(txt) {
  const s = String(txt || '').trim();
  if (!s) return null;
  const dateMatch = s.match(/(\d{1,2}\/\d{1,2}\/\d{4})/);
  return {
    id: '',
    titulo: s.substring(0, 200),
    tipo: 'Diário oficial',
    data: dateMatch ? dateMatch[1] : '',
    url: '',
    fonteOrigem: 'html',
  };
}

function mergePublicacoes(jsonList, diariosHtml) {
  const fromDiarios = (diariosHtml || [])
    .map(diarioTextoToPublicacao)
    .filter(Boolean);
  return mergeEntityLists(jsonList, fromDiarios, {
    getKey: publicacaoKey,
    getRecency: publicacaoRecency,
    mergePair: mergePublicacaoPair,
    tieBreak: (a, b) => (fieldScore(a, ['url', 'tipo']) >= fieldScore(b, ['url', 'tipo']) ? a : b),
  });
}

function publicacoesToDiariosStrings(publicacoes, limit = 15) {
  return (publicacoes || []).slice(0, limit).map((p) => {
    const t = `${p.titulo}${p.data ? ` — ${p.data}` : ''}`;
    return t.substring(0, 200);
  });
}

function mergePrefeituraSources(jsonBundle, htmlBundle) {
  const jc = (jsonBundle?.contratos || []).map((c) => ({ ...c, fonteOrigem: c.fonteOrigem || 'json' }));
  const hc = (htmlBundle?.contratos || []).map((c) => ({ ...c, fonteOrigem: c.fonteOrigem || 'html' }));
  const jl = filterValidLicitacoes((jsonBundle?.licitacoes || []).map((l) => ({ ...l, fonteOrigem: l.fonteOrigem || 'json' })));
  const hl = filterValidLicitacoes((htmlBundle?.licitacoes || []).map((l) => ({ ...l, fonteOrigem: l.fonteOrigem || 'html' })));
  const js = (jsonBundle?.secretarias || []).map((s) => ({ ...s, fonteOrigem: s.fonteOrigem || 'json' }));
  const hs = (htmlBundle?.secretarias || []).map((s) => ({ ...s, fonteOrigem: s.fonteOrigem || 'html' }));
  const jp = (jsonBundle?.publicacoes || []).map((p) => ({ ...p, fonteOrigem: p.fonteOrigem || 'json' }));
  const jg = (jsonBundle?.gestores || []).map((g) => ({ ...g, fonteOrigem: g.fonteOrigem || 'json' }));
  const hg = (htmlBundle?.gestores || []).map((g) => ({ ...g, fonteOrigem: g.fonteOrigem || 'html' }));

  const contratos = mergeContratos(jc, hc);
  const licitacoes = mergeLicitacoes(jl, hl);
  const secretarias = mergeSecretarias(js, hs);
  const publicacoes = mergePublicacoes(jp, htmlBundle?.diarios || []);
  const gestores = mergeGestores(jg, hg);

  const allSources = new Set([
    ...contratos.sourcesUsed,
    ...licitacoes.sourcesUsed,
    ...secretarias.sourcesUsed,
    ...publicacoes.sourcesUsed,
    ...gestores.sourcesUsed,
  ]);

  const mergedPublicacoes = publicacoes.items;

  return {
    contratos: contratos.items,
    licitacoes: licitacoes.items,
    secretarias: secretarias.items,
    publicacoes: mergedPublicacoes,
    gestores: gestores.items,
    diariosOficiais: publicacoesToDiariosStrings(mergedPublicacoes),
    fontesUtilizadas: [...allSources],
  };
}

module.exports = {
  parseBrazilianDate,
  mergeContratos,
  mergeLicitacoes,
  mergeSecretarias,
  mergeGestores,
  mergePublicacoes,
  mergePrefeituraSources,
  diarioTextoToPublicacao,
  publicacoesToDiariosStrings,
};
