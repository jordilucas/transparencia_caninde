'use strict';

const { parseIsoMs } = require('./scraper-camara-wp');

function pickNewer(a, b, field = 'modifiedAt') {
  const aMs = parseIsoMs(a?.[field]);
  const bMs = parseIsoMs(b?.[field]);
  if (aMs === bMs) return a;
  return aMs > bMs ? a : b;
}

function mergeParlamentar(wp, html) {
  if (!wp && !html) return null;
  if (!wp) return html;
  if (!html) return wp;

  const base = pickNewer(wp, html);
  const other = base === wp ? html : wp;

  return {
    ...base,
    nome: html.nome || wp.nome,
    nomeCompleto: html.nomeCompleto || wp.nomeCompleto || html.nome || wp.nome,
    partido: html.partido || wp.partido,
    cargo: html.cargo || wp.cargo,
    vinculo: html.vinculo || wp.vinculo,
    legislatura: html.legislatura || wp.legislatura,
    foto: html.foto || wp.foto,
    slug: html.slug || wp.slug,
    profileUrl: html.profileUrl || wp.profileUrl,
    totalMaterias: Math.max(html.totalMaterias || 0, wp.totalMaterias || 0),
    totalSessoes: Math.max(html.totalSessoes || 0, wp.totalSessoes || 0),
    modifiedAt: pickNewer(wp, html).modifiedAt || wp.modifiedAt || html.modifiedAt || '',
    contato: {
      email: html.contato?.email || wp.contato?.email || '',
      telefone: html.contato?.telefone || wp.contato?.telefone || '',
      whatsapp: html.contato?.whatsapp || wp.contato?.whatsapp || '',
      endereco: html.contato?.endereco || wp.contato?.endereco || '',
      horarioFuncionamento: html.contato?.horarioFuncionamento || wp.contato?.horarioFuncionamento || '',
    },
    biografia: html.biografia || wp.biografia || other.biografia || '',
  };
}

function mergeSessao(wp, html) {
  if (!wp && !html) return null;
  if (!wp) return html;
  if (!html) return wp;
  const base = pickNewer(wp, html);
  const other = base === wp ? html : wp;
  return {
    titulo: base.titulo || other.titulo,
    data: base.data || other.data,
    url: base.url || other.url,
    slug: base.slug || other.slug,
    resumo: base.resumo || other.resumo,
    modifiedAt: base.modifiedAt || other.modifiedAt || '',
  };
}

function mergeMateria(wp, html) {
  if (!wp && !html) return null;
  if (!wp) return html;
  if (!html) return wp;
  const base = pickNewer(wp, html);
  const other = base === wp ? html : wp;
  return {
    titulo: base.titulo || other.titulo,
    tipo: base.tipo || other.tipo,
    slug: base.slug || other.slug,
    url: base.url || other.url,
    autor: base.autor || other.autor,
    dataPublicacao: base.dataPublicacao || other.dataPublicacao,
    pdfUrl: base.pdfUrl || other.pdfUrl,
    resumo: base.resumo || other.resumo,
    modifiedAt: base.modifiedAt || other.modifiedAt || '',
  };
}

function mergeListByKey(itemsA, itemsB, keyFn, mergeFn) {
  const map = new Map();
  for (const item of [...itemsA, ...itemsB]) {
    const key = keyFn(item);
    if (!key) continue;
    const prev = map.get(key);
    map.set(key, prev ? mergeFn(item, prev) : item);
  }
  return [...map.values()];
}

function sortByModified(items) {
  return [...items].sort((a, b) => parseIsoMs(b.modifiedAt) - parseIsoMs(a.modifiedAt));
}

function mergeCamaraSources(wpBundle = {}, htmlBundle = {}) {
  const wpParl = wpBundle.parlamentares || [];
  const htmlParl = htmlBundle.parlamentares || [];

  const parlamentares = sortByModified(
    mergeListByKey(
      wpParl,
      htmlParl,
      (p) => p.slug || p.nome,
      mergeParlamentar,
    ),
  );

  const sessoes = sortByModified(
    mergeListByKey(
      wpBundle.sessoes || [],
      htmlBundle.sessoes || [],
      (s) => s.slug || s.titulo,
      mergeSessao,
    ),
  ).slice(0, 25);

  const materias = sortByModified(
    mergeListByKey(
      wpBundle.materias || [],
      htmlBundle.materias || [],
      (m) => m.slug || m.titulo,
      mergeMateria,
    ),
  ).slice(0, 25);

  const mesaHtml = htmlBundle.mesaDiretora || [];
  const mesaWp = wpBundle.mesaDiretora || [];
  const mesaDiretora = mesaHtml.length >= mesaWp.length ? mesaHtml : mesaWp;

  const fontesUtilizadas = [
    ...(wpBundle.fontesUtilizadas || []),
    ...(htmlBundle.fontesUtilizadas || []),
  ].filter((v, i, arr) => arr.indexOf(v) === i);

  const fonteParts = [wpBundle.fonte, htmlBundle.fonte].filter(Boolean);

  return {
    parlamentares,
    sessoes,
    materias,
    mesaDiretora,
    fonte: fonteParts.join(' + ') || htmlBundle.fonte || wpBundle.fonte,
    fontesUtilizadas,
  };
}

module.exports = {
  mergeCamaraSources,
  mergeParlamentar,
  mergeSessao,
  mergeMateria,
};
