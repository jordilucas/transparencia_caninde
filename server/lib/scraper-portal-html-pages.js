'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const { mergeEntitiesByRecency } = require('./entity-recency');

function resolveUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function scrapeTableDocuments($, pageUrl, defaults = {}) {
  const docs = [];
  $('table tbody tr, table tr').each((_, row) => {
    const cells = $(row).find('td');
    if (cells.length < 2) return;
    const link = $(row).find('a[href]').first();
    const href = link.attr('href') || '';
    const url = resolveUrl(href);
    const titulo = link.text().replace(/\s+/g, ' ').trim()
      || cells.map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get().join(' — ').substring(0, 200);
    if (!titulo || titulo.length < 4) return;
    const texts = cells.map((__, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
    const dateMatch = texts.join(' ').match(/(\d{2}\/\d{2}\/\d{4})/);
    const yearMatch = texts.join(' ').match(/\b(20\d{2})\b/);
    docs.push({
      id: String(defaults.idPrefix || '') + titulo.substring(0, 40).replace(/\W+/g, '-'),
      titulo: titulo.substring(0, 200),
      tipo: defaults.tipo || 'Documento',
      exercicio: yearMatch ? yearMatch[1] : String(defaults.exercicio || ''),
      data: dateMatch ? dateMatch[1] : '',
      url: url || pageUrl,
      fonteOrigem: defaults.fonteOrigem || 'html',
    });
  });
  return docs;
}

function scrapeListAnchors($, pageUrl, opts = {}) {
  const items = [];
  const selector = opts.selector || 'a[href*=".pdf"], a[href*="publicacoes.php"], a[href*="lrf.php"]';
  $(selector).each((_, el) => {
    const href = $(el).attr('href') || '';
    const url = resolveUrl(href);
    const titulo = $(el).text().replace(/\s+/g, ' ').trim();
    if (!titulo || titulo.length < 4 || !url) return;
    items.push({
      id: `${opts.idPrefix || 'doc'}-${titulo.substring(0, 30).replace(/\W+/g, '-')}`,
      titulo: titulo.substring(0, 200),
      tipo: opts.tipo || 'Publicação',
      exercicio: (titulo.match(/\b(20\d{2})\b/) || [])[1] || '',
      data: (titulo.match(/(\d{2}\/\d{2}\/\d{4})/) || [])[1] || '',
      url,
      fonteOrigem: 'html',
    });
  });
  return items;
}

async function scrapeLrfPortalPages(http, cheerio) {
  const paths = [
    '/lrf.php',
    '/lrf.php?cat=4',
    '/lrf.php?cat=7',
    '/lrf.php?cat=8',
    '/lrf.php?cat=9',
    '/lrf.php?cat=17',
    '/contasdegoverno.php',
    '/contasdegestao.php',
  ];
  const all = [];
  for (const path of paths) {
    try {
      const url = `${BASE}${path}`;
      const { data: html } = await http.get(url, {
        responseType: 'text',
        transformResponse: [(r) => r],
        timeout: 20_000,
        headers: { Referer: `${BASE}/acessoainformacao.php` },
      });
      const $ = cheerio.load(html);
      const tipo = path.includes('contasdegoverno') ? 'PCG'
        : path.includes('contasdegestao') ? 'PCS'
          : 'LRF';
      all.push(...scrapeTableDocuments($, url, { tipo, fonteOrigem: 'html-lrf' }));
      all.push(...scrapeListAnchors($, url, { tipo, idPrefix: tipo.toLowerCase() }));
    } catch (err) {
      console.warn(`[PortalHTML] ${path} indisponível:`, err.message);
    }
  }
  return mergeEntitiesByRecency([all]);
}

async function scrapeInstitucionalPortalPages(http, cheerio) {
  const specs = [
    { path: '/escolas.php', categoria: 'institucional', label: 'Escolas' },
    { path: '/veiculos.php', categoria: 'institucional', label: 'Veículos' },
    { path: '/conselhos.php', categoria: 'institucional', label: 'Conselhos' },
    { path: '/diariolista.php', categoria: 'legislativo', label: 'Diário oficial' },
    { path: '/publicacoes.php?grupo=&cat=21', categoria: 'compras', label: 'Convênios' },
  ];
  const links = [];
  for (const spec of specs) {
    try {
      const url = `${BASE}${spec.path}`;
      const { data: html } = await http.get(url, {
        responseType: 'text',
        transformResponse: [(r) => r],
        timeout: 20_000,
        headers: { Referer: `${BASE}/acessoainformacao.php` },
      });
      const $ = cheerio.load(html);
      const rows = $('table tbody tr, .list-group-item, ul li').length;
      const anchors = $('a[href]').length;
      const countHint = rows > 0 ? rows : anchors;
      links.push({
        titulo: `${spec.label}${countHint > 3 ? ` (${countHint} registros no portal)` : ''}`,
        url,
        categoria: spec.categoria,
        secao: 'Informações institucionais',
        fonteOrigem: 'html-resumo',
      });
    } catch (err) {
      console.warn(`[PortalHTML] ${spec.path} indisponível:`, err.message);
    }
  }
  return links;
}

module.exports = {
  BASE,
  mergeEntitiesByRecency,
  scrapeTableDocuments,
  scrapeLrfPortalPages,
  scrapeInstitucionalPortalPages,
};
