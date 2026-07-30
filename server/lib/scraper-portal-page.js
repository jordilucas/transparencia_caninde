'use strict';

const PREF_BASE = 'https://www.caninde.ce.gov.br';
const CAMARA_BASE = 'https://www.cmcaninde.ce.gov.br';
const { resolvePrefUrl, extractColStrongFields, extractPdfLinks } = require('./scraper-detail-prefeitura');
const { isAllowedOutboundUrl } = require('./allowed-hosts');

function decodePortalPageId(id) {
  if (!id || typeof id !== 'string') return '';
  if (/^https?:\/\//i.test(id)) return id;
  try {
    const normalized = id.replace(/-/g, '+').replace(/_/g, '/');
    const pad = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4));
    return Buffer.from(normalized + pad, 'base64').toString('utf8');
  } catch {
    return '';
  }
}

function detectOrigem(url) {
  if (/governotransparente\.com\.br/i.test(url)) return 'governo_transparente';
  if (/cmcaninde\.ce\.gov\.br/i.test(url)) return 'camara';
  if (/caninde\.ce\.gov\.br/i.test(url)) return 'prefeitura';
  return 'externo';
}

function extractMetaTitle($) {
  const og = $('meta[property="og:title"]').attr('content') || '';
  if (og.trim()) return og.trim().substring(0, 300);
  const title = $('title').first().text().replace(/\s+/g, ' ').trim();
  return title.substring(0, 300);
}

function extractResumoFromScope($, scope) {
  const parts = [];
  $(scope).find('p, li').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    if (t.length < 20 || /voltar|breadcrumb|cookie/i.test(t)) return;
    parts.push(t);
  });
  return parts.slice(0, 5).join('\n\n').substring(0, 2000);
}

function extractGenericLinks($, baseUrl) {
  const anexos = [];
  $('a[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    if (!href || href.startsWith('javascript:') || href.startsWith('#')) return;
    let url = href;
    if (!/^https?:\/\//i.test(url)) {
      url = href.startsWith('/') ? `${baseUrl}${href}` : `${baseUrl}/${href}`;
    }
    const label = $(el).text().replace(/\s+/g, ' ').trim();
    const isPdf = /\.pdf/i.test(url);
    const isRelevant = isPdf
      || /visualizar|documento|download|arquivo|pdf|planilha|csv/i.test(label);
    if (!isRelevant && !isPdf) return;
    if (anexos.some((a) => a.url === url)) return;
    anexos.push({
      titulo: (label || 'Documento').substring(0, 200),
      url,
      extensao: isPdf ? 'PDF' : '',
    });
  });
  return anexos.slice(0, 20);
}

function scrapePublicacaoDetail(html, cheerio, id) {
  const $ = cheerio.load(html);
  const titulo = $('h1.DataInforma').first().text().replace(/\s+/g, ' ').trim()
    || $('h1').first().text().replace(/\s+/g, ' ').trim();

  let resumo = '';
  $('center p').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    if (t.length > 10 && !/^\d{2}\/\d{2}\/\d{4}$/.test(t)) {
      resumo = t;
    }
  });

  let data = '';
  $('i.fa-calendar').each((_, el) => {
    const parent = $(el).parent().text().replace(/\s+/g, ' ').trim();
    const m = parent.match(/\d{2}\/\d{2}\/\d{4}/);
    if (m) data = m[0];
  });

  const campos = extractColStrongFields($, '.public_paginas');
  const anexos = extractPdfLinks($);
  $('a.btn-primary[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    if (!/\.pdf/i.test(href)) return;
    const url = resolvePrefUrl(href);
    if (!url || anexos.some((a) => a.url === url)) return;
    anexos.unshift({
      titulo: $(el).text().replace(/\s+/g, ' ').trim() || 'Documento',
      url,
      extensao: 'PDF',
    });
  });

  const tipo = resumo || titulo.split(':')[0]?.trim() || '';

  return {
    titulo,
    tipo,
    data,
    resumo,
    linkArquivo: anexos[0]?.url || '',
    camposExtras: campos,
    anexos,
  };
}

function scrapePortalPage(html, cheerio, url) {
  const $ = cheerio.load(html);
  const origem = detectOrigem(url);
  const baseUrl = origem === 'camara' ? CAMARA_BASE : PREF_BASE;
  let titulo = extractMetaTitle($);
  let resumo = '';
  let camposExtras = [];
  let anexos = [];

  if (origem === 'prefeitura') {
    titulo = titulo || $('.public_paginas h1').first().text().replace(/\s+/g, ' ').trim();
    camposExtras = extractColStrongFields($, '.public_paginas');
    resumo = extractResumoFromScope($, '.public_paginas');
    anexos = extractPdfLinks($);
  } else if (origem === 'camara') {
    titulo = titulo
      || $('.entry-title, h1.title, article h1').first().text().replace(/\s+/g, ' ').trim();
    resumo = extractResumoFromScope($, '.entry-content, article .content, .post-content');
    camposExtras = extractColStrongFields($, '.entry-content, article');
    anexos = extractGenericLinks($, CAMARA_BASE);
  } else if (origem === 'governo_transparente') {
    resumo = $('meta[name="description"]').attr('content') || '';
    if (!resumo) {
      resumo = 'Portal Governo Transparente — consulta interativa de dados financeiros e contratos.';
    }
  } else {
    resumo = $('meta[name="description"]').attr('content') || extractResumoFromScope($, 'main, article, body');
    anexos = extractGenericLinks($, new URL(url).origin);
  }

  if (!titulo) titulo = url.split('/').filter(Boolean).pop() || 'Página externa';

  return {
    titulo,
    url,
    resumo,
    origem,
    camposExtras,
    anexos,
    aviso: '',
  };
}

function mergePublicacaoDetail(listItem, scraped) {
  const base = listItem || {};
  const s = scraped || {};
  return {
    ...base,
    titulo: s.titulo || base.titulo || '',
    tipo: s.tipo || base.tipo || '',
    data: s.data || base.data || '',
    resumo: s.resumo || base.resumo || '',
    url: base.url || `${PREF_BASE}/publicacoes.php?id=${base.id || ''}`,
    linkArquivo: s.linkArquivo || base.linkArquivo || '',
    camposExtras: s.camposExtras?.length ? s.camposExtras : (base.camposExtras || []),
    anexos: s.anexos?.length ? s.anexos : (base.anexos || []),
  };
}

function mergePortalPageMeta(scraped, meta, url) {
  const s = scraped || {};
  const m = meta || {};
  const origem = s.origem || detectOrigem(url);
  let aviso = s.aviso || '';
  if (origem === 'governo_transparente' && !s.camposExtras?.length) {
    aviso = aviso || 'Os dados completos deste portal são interativos e podem exigir abertura no site oficial.';
  }
  if (!s.resumo && !s.camposExtras?.length && !s.anexos?.length) {
    aviso = aviso || 'Conteúdo limitado disponível para exibição inline. Use o botão abaixo para abrir no portal.';
  }
  return {
    titulo: s.titulo || m.titulo || 'Transparência',
    url,
    categoria: m.categoria || '',
    resumo: s.resumo || '',
    origem,
    camposExtras: s.camposExtras || [],
    anexos: s.anexos || [],
    aviso,
  };
}

function fallbackPortalPage(url, meta, err) {
  const m = meta || {};
  const origem = detectOrigem(url);
  const isGt = origem === 'governo_transparente';
  return {
    titulo: m.titulo || 'Transparência',
    url,
    categoria: m.categoria || '',
    resumo: m.titulo ? `Link oficial: ${m.titulo}` : '',
    origem,
    camposExtras: [],
    anexos: [],
    aviso: isGt
      ? 'O Governo Transparente exibe dados em painel interativo. Abra no navegador para consultar receitas, despesas e contratos.'
      : `Não foi possível carregar o conteúdo (${err?.message || 'erro de rede'}). Tente abrir diretamente no portal.`,
  };
}

function findLinkMeta(cache, url) {
  const lists = [
    ...(cache?.prefeitura?.linksTransparencia || []),
    ...(cache?.camara?.linksTransparencia || []),
  ];
  return lists.find((l) => l.url === url) || null;
}

function isAllowedPortalUrl(url) {
  return isAllowedOutboundUrl(url);
}

module.exports = {
  decodePortalPageId,
  isAllowedPortalUrl,
  scrapePublicacaoDetail,
  scrapePortalPage,
  mergePublicacaoDetail,
  mergePortalPageMeta,
  fallbackPortalPage,
  findLinkMeta,
};
