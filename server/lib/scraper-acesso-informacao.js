'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const HUB_URL = `${BASE}/acessoainformacao.php`;

const SKIP_HREF = /^(#|javascript:|mailto:|tel:)/i;
const SKIP_PATH = /\/(index\.php|acessibilidade|selos|pesquisa|mapadosite)(?:\?|$)/i;

function resolveHubUrl(href) {
  if (!href || typeof href !== 'string') return '';
  const t = href.trim();
  if (!t || SKIP_HREF.test(t)) return '';
  if (/^https?:\/\//i.test(t)) return t;
  return `${BASE}${t.startsWith('/') ? '' : '/'}${t}`;
}

function normalizeUrlKey(url) {
  return String(url || '').trim().toLowerCase().replace(/\/$/, '');
}

function inferCategoria(url, titulo, secao) {
  const primary = `${url} ${titulo}`.toLowerCase();
  const blob = `${url} ${titulo} ${secao}`.toLowerCase();
  if (/esic|ouvidoria|fale conosco|manifest/.test(blob)) return 'cidadania';
  if (/lrf|rreo|rgf|loa|ldo|ppa|pcg|pcs|contasde|contas de|prestação|fiscal/.test(primary)) return 'fiscal';
  if (/receita|despesa|emenda|cronolog|pix|financeir/.test(blob)) return 'financeiro';
  if (/folha|pessoal|servidor|quadro|diária|diaria|concurso|terceiriz|estagi/.test(blob)) return 'pessoal';
  if (/licita|contrato|convênio|convenio|compra|processo seletivo/.test(blob)) return 'compras';
  if (/obra/.test(blob)) return 'obras';
  if (/lrf|rreo|rgf|loa|ldo|ppa|pcg|pcs|contas de|fiscal|prestação/.test(blob)) return 'fiscal';
  if (/dados abertos|dadosabertos|exportar/.test(blob)) return 'dadosabertos';
  if (/medicamento|saúde|saude|unidade|regulação|regulacao/.test(blob)) return 'saude';
  if (/atricon|radar|dívida ativa|divida ativa|creche/.test(blob)) return 'atricon';
  if (/secretaria|institucional|prefeito|gestor|escola|veículo|veiculo|conselho/.test(blob)) return 'institucional';
  if (/lei|decreto|normativo|portaria|publica/.test(blob)) return 'legislativo';
  return 'portal';
}

function findSectionForElement($, el) {
  const direct = el.prevAll('h2, h3, h4').first();
  if (direct.length) {
    const text = direct.text().replace(/\s+/g, ' ').trim();
    if (text.length > 3) return text.substring(0, 120);
  }
  let parent = el.parent();
  for (let depth = 0; depth < 6 && parent.length; depth += 1) {
    const heading = parent.prevAll('h2, h3, h4').first();
    if (heading.length) {
      const text = heading.text().replace(/\s+/g, ' ').trim();
      if (text.length > 3) return text.substring(0, 120);
    }
    parent = parent.parent();
  }
  return 'Portal da transparência';
}

function scrapeAcessoInformacaoHub(html, cheerio) {
  const $ = cheerio.load(html);
  const links = [];
  const seen = new Set();

  $('a[href]').each((_, el) => {
    const href = $(el).attr('href') || '';
    const url = resolveHubUrl(href);
    if (!url || !url.includes('caninde.ce.gov.br')) return;
    if (SKIP_PATH.test(url)) return;

    const titulo = $(el).text().replace(/\s+/g, ' ').trim();
    if (!titulo || titulo.length < 3 || titulo.length > 140) return;
    if (/^(início|home|pesquisar|aceitar|recusar)$/i.test(titulo)) return;

    const key = normalizeUrlKey(url);
    if (seen.has(key)) return;
    seen.add(key);

    const secao = findSectionForElement($, $(el));
    links.push({
      titulo,
      url,
      categoria: inferCategoria(url, titulo, secao),
      secao,
      fonteOrigem: 'hub',
    });
  });

  links.sort((a, b) => {
    const sec = a.secao.localeCompare(b.secao, 'pt-BR');
    if (sec !== 0) return sec;
    return a.titulo.localeCompare(b.titulo, 'pt-BR');
  });

  return {
    hubUrl: HUB_URL,
    links,
    total: links.length,
  };
}

async function fetchAcessoInformacaoHub(http, cheerio) {
  try {
    const { data: html } = await http.get(HUB_URL, {
      responseType: 'text',
      transformResponse: [(r) => r],
      headers: { Referer: HUB_URL },
      timeout: 25_000,
    });
    return scrapeAcessoInformacaoHub(html, cheerio);
  } catch (err) {
    console.warn('[Hub] acessoainformacao indisponível:', err.message);
    return { hubUrl: HUB_URL, links: [], total: 0, error: err.message };
  }
}

module.exports = {
  BASE,
  HUB_URL,
  resolveHubUrl,
  inferCategoria,
  scrapeAcessoInformacaoHub,
  fetchAcessoInformacaoHub,
};
