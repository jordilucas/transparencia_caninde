'use strict';

const BASE = 'https://www.caninde.ce.gov.br';

function parseBRL(str) {
  if (!str) return 0;
  const n = parseFloat(String(str).replace(/[^\d,.-]/g, '').replace(/\./g, '').replace(',', '.'));
  return Number.isNaN(n) ? 0 : n;
}

function formatBRL(value) {
  if (value == null || Number.isNaN(value)) return '';
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function normalizeSecretariaKey(text) {
  return String(text || '')
    .replace(/\s+/g, ' ')
    .trim()
    .toUpperCase();
}

function scrapeFolhaMensal($) {
  const competencias = [];
  $('table tbody tr').each((_, row) => {
    const cells = $(row).find('td');
    if (cells.length < 4) return;

    const competencia = $(cells[0]).text().replace(/\s+/g, ' ').trim();
    if (!competencia || !/\d{4}/.test(competencia)) return;

    const proventos = $(cells[1]).text().replace(/[^\d,.-]/g, '').trim();
    const descontos = $(cells[2]).text().replace(/[^\d,.-]/g, '').trim();
    const liquido = $(cells[3]).text().replace(/[^\d,.-]/g, '').trim();
    if (!proventos && !liquido) return;

    competencias.push({
      competencia,
      tipoFolha: 'Normal',
      proventos: proventos ? formatBRL(parseBRL(proventos)) : '',
      descontos: descontos ? formatBRL(parseBRL(descontos)) : '',
      liquido: liquido ? formatBRL(parseBRL(liquido)) : '',
    });
  });
  return competencias;
}

function scrapeFolhaPorSetor($) {
  const map = new Map();

  $('span[itemprop="creditorName"]').each((_, el) => {
    const label = $(el).text().replace(/\s+/g, ' ').trim();
    if (!/FOLHA DE PAGAMENTO/i.test(label)) return;

    const row = $(el).closest('tr');
    if (!row.length) return;

    const orgao = row.find('[itemprop="managementUnitID"]').first().text().replace(/\s+/g, ' ').trim();
    const valorRaw = row.find('[itemprop="paymentAmount"]').first().text().replace(/[^\d,.-]/g, '').trim();
    const valor = parseBRL(valorRaw);
    if (!orgao || valor <= 0) return;

    const key = normalizeSecretariaKey(orgao);
    const codigo = (orgao.match(/^(\d+)/) || [])[1] || '';
    const current = map.get(key) || {
      secretaria: orgao,
      codigoOrgao: codigo,
      totalPagoNumerico: 0,
      quantidadePagamentos: 0,
    };
    current.totalPagoNumerico += valor;
    current.quantidadePagamentos += 1;
    map.set(key, current);
  });

  return [...map.values()]
    .map((item) => ({
      secretaria: item.secretaria,
      codigoOrgao: item.codigoOrgao,
      totalPago: formatBRL(item.totalPagoNumerico),
      totalPagoNumerico: Math.round(item.totalPagoNumerico * 100) / 100,
      quantidadePagamentos: item.quantidadePagamentos,
    }))
    .sort((a, b) => b.totalPagoNumerico - a.totalPagoNumerico);
}

async function scrapeFolhaPagamento(http, cheerio, year) {
  const exercicio = year || new Date().getFullYear();

  const [folhaSettled, pagSettled] = await Promise.allSettled([
    http.get(`${BASE}/folhadepagamento.php`, { responseType: 'text', transformResponse: [(r) => r] }),
    http.get(`${BASE}/lcpagamentos.php?ANO=${exercicio}`, { responseType: 'text', transformResponse: [(r) => r] }),
  ]);

  let competencias = [];
  let porSetor = [];

  if (folhaSettled.status === 'fulfilled') {
    const html = folhaSettled.value.data || '';
    const $folha = cheerio.load(html);
    competencias = scrapeFolhaMensal($folha);
  } else {
    console.warn('[Folha] folhadepagamento indisponível:', folhaSettled.reason?.message || folhaSettled.reason);
  }

  if (pagSettled.status === 'fulfilled') {
    const html = pagSettled.value.data || '';
    const $pag = cheerio.load(html);
    porSetor = scrapeFolhaPorSetor($pag);
  } else {
    console.warn('[Folha] lcpagamentos indisponível:', pagSettled.reason?.message || pagSettled.reason);
  }

  const totalNumerico = porSetor.reduce((sum, s) => sum + (s.totalPagoNumerico || 0), 0);

  return {
    exercicio,
    competencias,
    porSetor,
    totalPagoSetores: totalNumerico > 0 ? formatBRL(totalNumerico) : '',
    avisoPrivacidade:
      'Exibimos apenas totais mensais e valores pagos por secretaria/órgão (dados já publicados na transparência). '
      + 'Nomes, matrículas e contracheques individuais não são replicados neste app (LGPD). '
      + 'Consulte o portal oficial para detalhes nominais.',
    fonteUrl: `${BASE}/folhadepagamento.php`,
    fontePagamentosUrl: `${BASE}/lcpagamentos.php?ANO=${exercicio}`,
  };
}

module.exports = {
  BASE,
  parseBRL,
  formatBRL,
  scrapeFolhaMensal,
  scrapeFolhaPorSetor,
  scrapeFolhaPagamento,
};
