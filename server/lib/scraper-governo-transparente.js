'use strict';

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';
const GT_DADOS_ABERTOS_URL = `${GT_BASE}/dadosabertos/${GT_PREFEITURA_ID}?clean=false`;
const GT_REFERER = GT_DADOS_ABERTOS_URL;

const TOP_RECEITA_CODIGO = /^\d{3}\.0\.0\.0\.00\.0\.0\.00\.00\.00$/;
const FOLHA_FORNECEDOR = /folha de pagamento/i;

function yearToExer(year) {
  return Number(year) - 2009;
}

function parseBRL(str) {
  if (!str) return 0;
  const n = parseFloat(String(str).replace(/[^\d,.-]/g, '').replace(/\./g, '').replace(',', '.'));
  return Number.isNaN(n) ? 0 : n;
}

function formatBRL(value) {
  if (value == null || Number.isNaN(value)) return '';
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function sumReceitasTopLevel(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    return { arrecadada: 0, prevista: 0 };
  }
  let arrecadada = 0;
  let prevista = 0;
  for (const row of rows) {
    const codigo = String(row.codigo || '').trim();
    if (!TOP_RECEITA_CODIGO.test(codigo)) continue;
    arrecadada += Number(row.saldo) || 0;
    prevista += Number(row.previsao) || 0;
  }
  return { arrecadada, prevista };
}

function parseDespesaTotalFromTitle(html) {
  const text = String(html || '');
  const match = text.match(/Total das despesas:\s*R\$\s*([\d.]+,\d{2})/i);
  if (!match) return 0;
  return parseBRL(match[1]);
}

function mapTopFornecedores(rows, limit = 8) {
  if (!Array.isArray(rows)) return [];
  return rows
    .filter((row) => row && !FOLHA_FORNECEDOR.test(String(row.nome || '')))
    .sort((a, b) => (Number(b.valor) || 0) - (Number(a.valor) || 0))
    .slice(0, limit)
    .map((row) => ({
      nome: String(row.nome || '').trim(),
      cnpj: String(row.cpfcnpj || '').trim(),
      valor: formatBRL(Number(row.valor) || 0),
    }));
}

async function fetchGtJson(http, path) {
  const url = path.startsWith('http') ? path : `${GT_BASE}${path}`;
  const res = await http.get(url, {
    headers: {
      Referer: GT_REFERER,
      Accept: 'application/json, text/plain, */*',
    },
    timeout: 45_000,
    validateStatus: (status) => status >= 200 && status < 400,
  });
  return res.data;
}

async function scrapeGtResumo(http, exercicio = new Date().getFullYear()) {
  const exer = yearToExer(exercicio);
  const empty = {
    receitaArrecadada: '',
    receitaPrevista: '',
    despesaPaga: '',
    topFornecedores: [],
    gtDadosAbertosUrl: GT_DADOS_ABERTOS_URL,
    gtFonte: 'Governo Transparente',
    gtDisponivel: false,
  };

  try {
    const [receitasSettled, fornecedoresSettled, despesaSettled] = await Promise.allSettled([
      fetchGtJson(http, `/portal/api/v2/json/receitasprevarrec/${GT_PREFEITURA_ID}?exer=${exer}`),
      fetchGtJson(http, `/portal/api/v1/json/totalporfornecedor/${GT_PREFEITURA_ID}?exer=${exer}`),
      http.get(
        `${GT_BASE}/transparencia/${GT_PREFEITURA_ID}/dadosabertos/consultarpagdesporc?clean=false`,
        {
          headers: { Referer: GT_REFERER, Accept: 'text/html' },
          timeout: 30_000,
          validateStatus: (status) => status >= 200 && status < 400,
        },
      ),
    ]);

    const receitasRows = receitasSettled.status === 'fulfilled' ? receitasSettled.value : [];
    const fornecedoresRows = fornecedoresSettled.status === 'fulfilled' ? fornecedoresSettled.value : [];
    const despesaHtml = despesaSettled.status === 'fulfilled' ? despesaSettled.value?.data : '';

    const { arrecadada, prevista } = sumReceitasTopLevel(receitasRows);
    const despesaPaga = parseDespesaTotalFromTitle(despesaHtml);
    const topFornecedores = mapTopFornecedores(fornecedoresRows);

    const hasReceita = arrecadada > 0;
    const hasDespesa = despesaPaga > 0;
    const hasFornecedores = topFornecedores.length > 0;

    if (!hasReceita && !hasDespesa && !hasFornecedores) {
      return empty;
    }

    return {
      receitaArrecadada: hasReceita ? formatBRL(arrecadada) : '',
      receitaPrevista: prevista > 0 ? formatBRL(prevista) : '',
      despesaPaga: hasDespesa ? formatBRL(despesaPaga) : '',
      topFornecedores,
      gtDadosAbertosUrl: GT_DADOS_ABERTOS_URL,
      gtFonte: 'Governo Transparente',
      gtDisponivel: true,
    };
  } catch (err) {
    console.warn('[GT] resumo indisponível:', err.message);
    return empty;
  }
}

module.exports = {
  GT_PREFEITURA_ID,
  GT_BASE,
  GT_DADOS_ABERTOS_URL,
  yearToExer,
  parseBRL,
  formatBRL,
  sumReceitasTopLevel,
  parseDespesaTotalFromTitle,
  mapTopFornecedores,
  scrapeGtResumo,
};
