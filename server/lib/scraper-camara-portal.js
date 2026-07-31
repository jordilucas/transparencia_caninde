'use strict';

const BASE = 'https://www.cmcaninde.ce.gov.br';

function resolveUrl(href) {
  if (!href || typeof href !== 'string') return '';
  if (/^https?:\/\//i.test(href)) return href;
  return `${BASE}${href.startsWith('/') ? '' : '/'}${href}`;
}

function scrapeDocumentList(html, cheerio, categoria) {
  const $ = cheerio.load(html);
  const docs = [];
  const seen = new Set();

  function pushDoc(titulo, href, data) {
    const url = resolveUrl(href);
    if (!titulo || titulo.length < 4 || !url || seen.has(url)) return;
    seen.add(url);
    docs.push({
      titulo: titulo.substring(0, 160),
      data: (data || '').substring(0, 60),
      url,
      categoria,
    });
  }

  $('article, .post, .entry-content, main').find('a[href]').each((_, el) => {
    if (docs.length >= 30) return false;
    const href = $(el).attr('href') || '';
    if (!href || href === '#' || /javascript:|mailto:/i.test(href)) return;
    const titulo = $(el).text().replace(/\s+/g, ' ').trim();
    if (titulo.length < 5) return;
    const block = $(el).closest('article, li, .post, tr, .card');
    const data = block.find('time, .date, small, .meta').first().text().replace(/\s+/g, ' ').trim();
    pushDoc(titulo, href, data);
  });

  if (docs.length === 0) {
    $('h2 a, h3 a, .entry-title a').each((_, el) => {
      if (docs.length >= 30) return false;
      const href = $(el).attr('href') || '';
      const titulo = $(el).text().replace(/\s+/g, ' ').trim();
      pushDoc(titulo, href, '');
    });
  }

  return docs;
}

async function scrapeCamaraPortal(http, cheerio) {
  const [licRes, conRes] = await Promise.allSettled([
    http.get(`${BASE}/caninde-transparente/licitacoes/`),
    http.get(`${BASE}/caninde-transparente/contratos/`),
  ]);

  const licitacoes = licRes.status === 'fulfilled'
    ? scrapeDocumentList(licRes.value.data, cheerio, 'licitacao')
    : [];
  const contratos = conRes.status === 'fulfilled'
    ? scrapeDocumentList(conRes.value.data, cheerio, 'contrato')
    : [];

  const fontesUtilizadas = [];
  if (licitacoes.length) fontesUtilizadas.push('portal-licitacoes');
  if (contratos.length) fontesUtilizadas.push('portal-contratos');

  return {
    documentosTransparencia: [...licitacoes, ...contratos],
    fontesUtilizadas,
  };
}

module.exports = {
  BASE,
  scrapeCamaraPortal,
  scrapeDocumentList,
  resolveUrl,
};
