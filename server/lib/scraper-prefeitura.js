'use strict';

const BASE = 'https://www.caninde.ce.gov.br';
const { parseContratoHtmlRow } = require('./contrato-html');

function scrapeSecretariasFromHtml($m) {
  const secretarias = [];
  const seen = new Set();
  $m('a[href*="secretaria.php?sec="]').each((_, el) => {
    const href = $m(el).attr('href') || '';
    const m = href.match(/sec=(\d+)/);
    const id = m ? m[1] : '';
    const nome = $m(el).text().trim();
    if (!nome || seen.has(id || nome)) return;
    seen.add(id || nome);
    secretarias.push({
      id,
      nome,
      secretario: '',
      url: href.startsWith('http') ? href : `${BASE}/${href.replace(/^\//, '')}`,
      contato: { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' },
    });
  });
  return secretarias;
}

function scrapeContratos($c, limit = 30) {
  const contratos = [];
  $c('table tbody tr').each((i, row) => {
    if (i >= limit) return false;
    const cols = $c(row).find('td');
    if (cols.length < 3) return;
    const texts = cols.map((_, td) => $c(td).text().trim()).get();
    const link = $c(row).find('a').attr('href') || '';
    const parsed = parseContratoHtmlRow(texts, link);
    if (!parsed || (!parsed.numero && !parsed.objeto)) return;
    contratos.push({
      ...parsed,
      url: parsed.url.startsWith('http')
        ? parsed.url
        : (parsed.url ? `${BASE}/${parsed.url.replace(/^\//, '')}` : ''),
    });
  });
  return contratos;
}

function scrapeLicitacoes($l, limit = 25) {
  const licitacoes = [];
  $l('table tbody tr').each((i, row) => {
    if (i >= limit) return false;
    const cols = $l(row).find('td');
    if (cols.length >= 2) {
      licitacoes.push({
        numero: $l(cols[0]).text().trim(),
        modalidade: $l(cols[1]).text().trim(),
        objeto: $l(cols[2])?.text().trim().substring(0, 200) || '',
        situacao: $l(cols[3])?.text().trim() || '',
        url: '',
      });
    }
  });
  return licitacoes;
}

function scrapeDiarios($d, limit = 15) {
  const diarios = [];
  $d('table tbody tr, .lista-publicacoes li, ul.publicacoes li').each((i, el) => {
    if (i >= limit) return false;
    const txt = $d(el).text().trim();
    if (txt.length > 5) diarios.push(txt.substring(0, 200));
  });
  return diarios;
}

async function scrapePrefeituraHtml(http, cheerio, limits = {}) {
  const maxContratos = limits.contratos ?? 30;
  const maxLicitacoes = limits.licitacoes ?? 25;
  const maxDiarios = limits.diarios ?? 15;
  const maxSecretarias = limits.secretarias ?? 20;

  const { data: htmlMain } = await http.get(`${BASE}/acessoainformacao.php`);
  const $m = cheerio.load(htmlMain);

  const [{ data: htmlContratos }, { data: htmlLicit }, { data: htmlDiario }] = await Promise.all([
    http.get(`${BASE}/contratos.php`),
    http.get(`${BASE}/licitacao.php`),
    http.get(`${BASE}/diariolista.php`),
  ]);

  const tag = (list) => list.map((item) => ({ ...item, fonteOrigem: 'html' }));

  return {
    contratos: tag(scrapeContratos(cheerio.load(htmlContratos), maxContratos)),
    licitacoes: tag(scrapeLicitacoes(cheerio.load(htmlLicit), maxLicitacoes)),
    secretarias: tag(scrapeSecretariasFromHtml($m).slice(0, maxSecretarias)),
    diarios: scrapeDiarios(cheerio.load(htmlDiario), maxDiarios),
    fonte: `${BASE}/acessoainformacao.php (HTML)`,
  };
}

module.exports = {
  BASE,
  scrapeSecretariasFromHtml,
  scrapeContratos,
  scrapeLicitacoes,
  scrapeDiarios,
  scrapePrefeituraHtml,
};
