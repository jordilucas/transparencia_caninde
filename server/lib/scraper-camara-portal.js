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

function scrapeDocumentoCamaraDetail(html, cheerio, url) {
  const $ = cheerio.load(html);
  const titulo = $('h1, h2, .entry-title').first().text().replace(/\s+/g, ' ').trim();
  const resumoParts = [];
  $('.entry-content p, article p, main p').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    if (t.length >= 25 && !/cookie|compartilhar/i.test(t)) resumoParts.push(t);
  });
  const resumo = resumoParts.slice(0, 4).join('\n\n').substring(0, 2000);
  const camposExtras = [];
  $('.entry-content p, article p').each((_, el) => {
    const t = $(el).text().replace(/\s+/g, ' ').trim();
    const m = t.match(/^([^:]{3,40}):\s*(.+)$/);
    if (m && m[2].length > 2) {
      camposExtras.push({ rotulo: m[1].trim(), valor: m[2].trim().substring(0, 400) });
    }
  });
  const anexos = [];
  $('a[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    if (!/\.pdf|download|documento|arquivo/i.test(href)) return;
    const docUrl = resolveUrl(href);
    if (!docUrl || anexos.some((a) => a.url === docUrl)) return;
    anexos.push({
      titulo: $(el).text().replace(/\s+/g, ' ').trim() || 'Documento',
      url: docUrl,
      extensao: /\.pdf/i.test(docUrl) ? 'PDF' : '',
    });
  });
  let categoria = 'documento';
  if (/licitac/i.test(url)) categoria = 'licitacao';
  else if (/contrat/i.test(url)) categoria = 'contrato';
  const data = $('time, .date, .meta').first().text().replace(/\s+/g, ' ').trim().substring(0, 60);
  return {
    titulo: titulo.substring(0, 160),
    data,
    url,
    categoria,
    resumo,
    camposExtras: camposExtras.slice(0, 12),
    anexos: anexos.slice(0, 15),
  };
}

function mergeDocumentoCamara(listItem, scraped) {
  if (!scraped) return listItem || null;
  if (!listItem) return scraped;
  return {
    ...listItem,
    titulo: scraped.titulo || listItem.titulo,
    data: scraped.data || listItem.data,
    url: scraped.url || listItem.url,
    categoria: scraped.categoria || listItem.categoria,
    resumo: scraped.resumo || listItem.resumo || '',
    camposExtras: scraped.camposExtras?.length ? scraped.camposExtras : (listItem.camposExtras || []),
    anexos: scraped.anexos?.length ? scraped.anexos : (listItem.anexos || []),
  };
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
  scrapeDocumentoCamaraDetail,
  mergeDocumentoCamara,
  resolveUrl,
};
