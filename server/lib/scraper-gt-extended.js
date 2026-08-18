'use strict';

const cheerio = require('cheerio');
const { formatBRL } = require('./brl');

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';
const GT_DADOS_ABERTOS_URL = `${GT_BASE}/dadosabertos/${GT_PREFEITURA_ID}?clean=false`;
const GT_REFERER = GT_DADOS_ABERTOS_URL;
const TOP_RECEITA_CODIGO = /^\d{3}\.0\.0\.0\.00\.0\.0\.00\.00\.00$/;
const SAAE_ORGAO = /saae|044\s*-|servi[cç]o aut[oô]nomo de [aá]gua|[aá]gua e esgoto/i;

function yearToExer(year) {
  return Number(year) - 2009;
}

function buildPeriodoExercicio(exercicio, dataFim) {
  if (!dataFim) return `Exercício ${exercicio}`;
  const fimAno = dataFim.slice(-4);
  if (String(exercicio) === fimAno) {
    return `01/01/${exercicio} a ${dataFim}`;
  }
  return `Exercício ${exercicio}`;
}

function exercicioDateRange(exercicio) {
  const year = Number(exercicio) || new Date().getFullYear();
  return { inicio: `01/01/${year}`, fim: `31/12/${year}` };
}

function unwrapGtObject(data) {
  if (!data || typeof data !== 'object') return null;
  if (data.Resultado && typeof data.Resultado === 'object') return data.Resultado;
  return data;
}

function parseRemessa(data) {
  const raw = unwrapGtObject(data);
  if (!raw) return null;
  const ultima = String(raw.dataUltimaRemessa || raw.dataRemessa || '').trim();
  const primeiro = String(raw.dataPrimeiroMovimento || '').trim();
  const ultimo = String(raw.dataUltimoMovimento || '').trim();
  if (!ultima && !ultimo) return null;
  return {
    dataUltimaRemessa: ultima,
    dataPrimeiroMovimento: primeiro,
    dataUltimoMovimento: ultimo,
    dataConsulta: String(raw.dataConsulta || '').trim(),
  };
}

function rowReceitaValor(row) {
  const n = Number(row?.arrecadado ?? row?.saldo ?? row?.valor ?? 0);
  return Number.isNaN(n) ? 0 : n;
}

function rowReceitaNome(row) {
  const nome = String(row?.especificacao || row?.nome || row?.descricao || '').trim();
  if (nome) return nome;
  const codigo = String(row?.codigo || '').trim();
  return codigo ? `Receita ${codigo.split('.')[0]}` : 'Receita';
}

function mapReceitasTopRubricas(rows = [], limit = 5) {
  if (!Array.isArray(rows)) return [];
  return rows
    .filter((row) => row && TOP_RECEITA_CODIGO.test(String(row.codigo || '').trim()))
    .map((row) => ({
      nome: rowReceitaNome(row),
      codigo: String(row.codigo || '').trim(),
      valorNumerico: rowReceitaValor(row),
      valorFormatado: formatBRL(rowReceitaValor(row)),
    }))
    .filter((row) => row.valorNumerico > 0)
    .sort((a, b) => b.valorNumerico - a.valorNumerico)
    .slice(0, limit);
}

function findSaaeOrgaoId(orgs = []) {
  if (!Array.isArray(orgs)) return null;
  for (const org of orgs) {
    const nome = String(org?.nome || '').trim();
    const id = org?.id ?? org?.idOrgao;
    if (id == null) continue;
    if (/^\s*044\b/.test(nome) || SAAE_ORGAO.test(nome)) {
      return { id: String(id), nome };
    }
  }
  return null;
}

function parsePagamentoValor(row) {
  const n = Number(row?.valor ?? 0);
  if (!Number.isNaN(n) && n !== 0) return Math.abs(n);
  return 0;
}

function mapPagamentosRows(rows = [], limit = 25) {
  if (!Array.isArray(rows)) return [];
  return rows
    .map((row) => {
      const valor = parsePagamentoValor(row);
      if (valor <= 0) return null;
      const registro = String(row?.registro || '').toLowerCase();
      if (registro.includes('anul')) return null;
      return {
        data: String(row?.dataMovimento || row?.data || '').trim(),
        credor: String(row?.credor || row?.nome || '').trim(),
        valor: formatBRL(valor),
        valorNumerico: valor,
        natureza: String(row?.naturezaDaDespesa || row?.natureza || '').trim(),
        documento: String(row?.numeroDocumento || row?.numeroEmpenho || '').trim(),
      };
    })
    .filter(Boolean)
    .sort((a, b) => b.valorNumerico - a.valorNumerico)
    .slice(0, limit)
    .map(({ valorNumerico, ...rest }) => rest);
}

function sumPagamentos(rows = []) {
  if (!Array.isArray(rows)) return 0;
  return rows.reduce((sum, row) => {
    const registro = String(row?.registro || '').toLowerCase();
    if (registro.includes('anul')) return sum;
    return sum + parsePagamentoValor(row);
  }, 0);
}

function parseConveniosHtml(html, limit = 20) {
  const $ = cheerio.load(String(html || ''));
  const items = [];

  $('table').each((_, table) => {
    const headers = [];
    $(table).find('tr').first().find('th, td').each((__, cell) => {
      headers.push($(cell).text().replace(/\s+/g, ' ').trim().toLowerCase());
    });

    $(table).find('tr').slice(1).each((__, row) => {
      const cells = $(row).find('td').map((___, td) => $(td).text().replace(/\s+/g, ' ').trim()).get();
      if (cells.length < 2) return;

      const link = $(row).find('a[href]').first().attr('href') || '';
      const joined = cells.join(' | ');
      if (!/\d/.test(joined) && !/r\$/i.test(joined)) return;

      let numero = cells[0] || '';
      let objeto = cells[1] || '';
      let parceiro = cells.length > 2 ? cells[2] : '';
      let valor = '';
      let situacao = '';

      for (const cell of cells) {
        if (/r\$\s*[\d.]+\,\d{2}/i.test(cell)) valor = cell.match(/r\$\s*[\d.]+\,\d{2}/i)[0];
        if (/vigen|ativo|encerr|homolog/i.test(cell.toLowerCase())) situacao = cell;
      }
      if (!valor && cells.length >= 4) valor = cells[3];
      if (!situacao && cells.length >= 5) situacao = cells[4];

      items.push({
        numero,
        objeto,
        parceiro,
        valor: valor || '',
        situacao: situacao || '',
        url: link.startsWith('http') ? link : (link ? `${GT_BASE}${link.startsWith('/') ? '' : '/'}${link}` : ''),
      });
    });
  });

  const seen = new Set();
  return items
    .filter((item) => {
      const key = `${item.numero}|${item.objeto}|${item.valor}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return item.numero || item.objeto;
    })
    .slice(0, limit);
}

async function fetchGtJsonTryPaths(http, paths) {
  for (const path of paths) {
    try {
      const url = path.startsWith('http') ? path : `${GT_BASE}${path}`;
      const res = await http.get(url, {
        headers: { Referer: GT_REFERER, Accept: 'application/json, text/plain, */*' },
        timeout: 45_000,
        validateStatus: (status) => status >= 200 && status < 400,
      });
      if (res.data != null && typeof res.data === 'object') return res.data;
    } catch {
      /* try next */
    }
  }
  return null;
}

async function fetchGtHtml(http, path) {
  const url = path.startsWith('http') ? path : `${GT_BASE}${path}`;
  const res = await http.get(url, {
    headers: { Referer: GT_REFERER, Accept: 'text/html' },
    timeout: 45_000,
    validateStatus: (status) => status >= 200 && status < 400,
  });
  return res.data;
}

async function fetchPagamentosPorOrgao(http, exercicio, orgaoId, maxPages = 2, pageSize = 80) {
  const exer = yearToExer(exercicio);
  const { inicio, fim } = exercicioDateRange(exercicio);
  const all = [];

  for (let page = 1; page <= maxPages; page += 1) {
    const q = `page=${page}&pagesize=${pageSize}&exer=${exer}&inicio=${encodeURIComponent(inicio)}&fim=${encodeURIComponent(fim)}&orgao=${encodeURIComponent(orgaoId)}`;
    const paths = [
      `/portal/api/v1/json/pagamentos/${GT_PREFEITURA_ID}?${q}`,
      `/transparencia/api/v1/json/pagamentos/${GT_PREFEITURA_ID}?${q}`,
    ];
    const data = await fetchGtJsonTryPaths(http, paths);
    const rows = Array.isArray(data) ? data : (Array.isArray(data?.Resultado) ? data.Resultado : []);
    if (!rows.length) break;
    all.push(...rows);
    if (rows.length < pageSize) break;
  }

  return all;
}

async function scrapeGtExtended(http, exercicio = new Date().getFullYear(), receitasRows = []) {
  const exer = yearToExer(exercicio);
  const { inicio, fim } = exercicioDateRange(exercicio);

  const remessaPaths = [
    `/portal/api/v1/json/remessas/${GT_PREFEITURA_ID}`,
    `/transparencia/api/v1/json/remessas/${GT_PREFEITURA_ID}`,
  ];
  const orgaosPaths = [
    `/portal/api/v1/json/${GT_PREFEITURA_ID}/orgaosdoexercicio/${exer}`,
    `/transparencia/api/v1/json/${GT_PREFEITURA_ID}/orgaosdoexercicio/${exer}`,
  ];

  const [remessaData, orgaosData, conveniosHtmlSettled] = await Promise.allSettled([
    fetchGtJsonTryPaths(http, remessaPaths),
    fetchGtJsonTryPaths(http, orgaosPaths),
    fetchGtHtml(http, `/transparencia/${GT_PREFEITURA_ID}/consultarconvenio?clean=false`),
  ]);

  const remessa = remessaData.status === 'fulfilled' ? parseRemessa(remessaData.value) : null;
  const orgaosRaw = orgaosData.status === 'fulfilled' ? orgaosData.value : null;
  const orgaos = Array.isArray(orgaosRaw) ? orgaosRaw : (Array.isArray(orgaosRaw?.Resultado) ? orgaosRaw.Resultado : []);
  const saaeOrgao = findSaaeOrgaoId(orgs);

  let pagamentosRaw = [];
  if (saaeOrgao?.id) {
    try {
      pagamentosRaw = await fetchPagamentosPorOrgao(http, exercicio, saaeOrgao.id);
    } catch (err) {
      console.warn('[GT] pagamentos SAAE indisponíveis:', err.message);
    }
  }

  const conveniosHtml = conveniosHtmlSettled.status === 'fulfilled' ? conveniosHtmlSettled.value : '';
  const convenios = parseConveniosHtml(conveniosHtml);

  const receitasPorRubrica = mapReceitasTopRubricas(receitasRows);
  const pagamentosSaae = mapPagamentosRows(pagamentosRaw);
  const totalPagamentosSaae = sumPagamentos(pagamentosRaw);

  let dadosAtualizadosEm = remessa?.dataUltimaRemessa || remessa?.dataUltimoMovimento || '';
  const periodoReferencia = buildPeriodoExercicio(exercicio, dadosAtualizadosEm);

  return {
    remessa,
    convenios,
    receitasPorRubrica,
    pagamentosSaae,
    totalPagamentosSaae: totalPagamentosSaae > 0 ? formatBRL(totalPagamentosSaae) : '',
    quantidadePagamentosSaae: pagamentosRaw.length,
    saaeOrgaoNome: saaeOrgao?.nome || '',
    saaeOrgaoId: saaeOrgao?.id || '',
    dadosAtualizadosEm,
    periodoReferencia,
    gtConveniosUrl: `${GT_BASE}/transparencia/${GT_PREFEITURA_ID}/consultarconvenio?clean=false`,
  };
}

module.exports = {
  TOP_RECEITA_CODIGO,
  exercicioDateRange,
  parseRemessa,
  mapReceitasTopRubricas,
  findSaaeOrgaoId,
  mapPagamentosRows,
  sumPagamentos,
  parseConveniosHtml,
  scrapeGtExtended,
};
