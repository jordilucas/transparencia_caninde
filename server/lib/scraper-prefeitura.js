'use strict';

const BASE = 'https://www.caninde.ce.gov.br';

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
    if (cols.length >= 3) {
      const link = $c(row).find('a').attr('href') || '';
      contratos.push({
        numero: $c(cols[0]).text().trim(),
        objeto: $c(cols[1]).text().trim().substring(0, 200),
        valor: $c(cols[2]).text().trim(),
        empresa: $c(cols[3])?.text().trim() || '',
        data: $c(cols[4])?.text().trim() || '',
        url: link.startsWith('http') ? link : (link ? `${BASE}/${link.replace(/^\//, '')}` : ''),
      });
    }
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

module.exports = {
  BASE,
  scrapeSecretariasFromHtml,
  scrapeContratos,
  scrapeLicitacoes,
  scrapeDiarios,
};
