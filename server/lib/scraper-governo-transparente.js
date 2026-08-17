'use strict';

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';
const GT_DADOS_ABERTOS_URL = `${GT_BASE}/dadosabertos/${GT_PREFEITURA_ID}?clean=false`;
const GT_REFERER = GT_DADOS_ABERTOS_URL;

const TOP_RECEITA_CODIGO = /^\d{3}\.0\.0\.0\.00\.0\.0\.00\.00\.00$/;
const FOLHA_FORNECEDOR = /folha de pagamento/i;
const FOLHA_LABEL_PREFIX = /^folha de pagamento\s*-?\s*/i;

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

function parsePeriodoPortal(html) {
  const text = String(html || '');
  const match = text.match(/\(\s*(\d{2}\/\d{2}\/\d{4})\s+a\s+(\d{2}\/\d{2}\/\d{4})\s*\)/);
  if (!match) return { inicio: '', fim: '' };
  return { inicio: match[1], fim: match[2] };
}

function parseDadosAtualizados(html) {
  const text = String(html || '');
  const match = text.match(/Dados atualizados\s+(\d{2}\/\d{2}\/\d{4})/i);
  return match ? match[1] : '';
}

function buildPeriodoExercicio(exercicio, dataFim) {
  if (!dataFim) return `Exercício ${exercicio}`;
  const fimAno = dataFim.slice(-4);
  if (String(exercicio) === fimAno) {
    return `01/01/${exercicio} a ${dataFim}`;
  }
  return `Exercício ${exercicio}`;
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

function mapFolhaGtSetores(rows) {
  if (!Array.isArray(rows)) return [];
  const map = new Map();

  for (const row of rows) {
    if (!row || !FOLHA_FORNECEDOR.test(String(row.nome || ''))) continue;
    const valor = Number(row.valor) || 0;
    if (valor <= 0) continue;

    const label = String(row.nome || '')
      .replace(FOLHA_LABEL_PREFIX, '')
      .replace(/\s+/g, ' ')
      .trim() || 'Folha de pagamento';
    const key = label.toUpperCase();
    const codigoMatch = label.match(/^(\d{2})\s*-/) || label.match(/^(\d{2})\b/);
    const current = map.get(key) || {
      secretaria: label,
      codigoOrgao: codigoMatch ? codigoMatch[1] : '',
      totalPagoNumerico: 0,
      quantidadePagamentos: 0,
    };
    current.totalPagoNumerico += valor;
    current.quantidadePagamentos += 1;
    map.set(key, current);
  }

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

function calcPercentualArrecadacao(arrecadada, prevista) {
  if (!prevista || prevista <= 0 || !arrecadada) return '';
  const pct = (arrecadada / prevista) * 100;
  return `${pct.toLocaleString('pt-BR', { maximumFractionDigits: 1, minimumFractionDigits: 1 })}%`;
}

function buildGtFinanceLinks(id = GT_PREFEITURA_ID) {
  const base = `${GT_BASE}/transparencia/${id}`;
  const q = '?clean=false';
  return [
    {
      titulo: 'Painel de receitas',
      url: `${GT_BASE}/transparencia/receitas/${id}${q}`,
      categoria: 'financeiro',
    },
    {
      titulo: 'Receita orçamentária arrecadada',
      url: `${base}/consultarrecorcarrecadada${q}`,
      categoria: 'receita',
    },
    {
      titulo: 'Receita prevista × arrecadada',
      url: `${base}/consultarrecprevar${q}`,
      categoria: 'receita',
    },
    {
      titulo: 'Receita extraorçamentária',
      url: `${base}/consultarrecextraorc${q}&tipotrans=N`,
      categoria: 'receita',
    },
    {
      titulo: 'Transferências intragovernamentais (entradas)',
      url: `${base}/consultarrecextraorc${q}&tipotrans=S`,
      categoria: 'receita',
    },
    {
      titulo: 'Painel de despesas',
      url: `${GT_BASE}/transparencia/despesas/opcoes/${id}${q}`,
      categoria: 'financeiro',
    },
    {
      titulo: 'Despesas empenhadas',
      url: `${base}/consultarempenho${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Despesas liquidadas',
      url: `${base}/consultarliqdesporc${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Despesas pagas',
      url: `${base}/consultarpagdesporc${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Despesas por fornecedor',
      url: `${base}/consultardespesafornecedor${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Lista de fornecedores',
      url: `${base}/consultarlistadefornecedores${q}`,
      categoria: 'despesa',
    },
  ];
}

function buildGtPortalLinks(id = GT_PREFEITURA_ID) {
  const base = `${GT_BASE}/transparencia/${id}`;
  const q = '?clean=false';
  return [
    {
      titulo: 'Portal Governo Transparente',
      url: `${GT_BASE}/${id}${q}`,
      categoria: 'portal',
    },
    {
      titulo: 'Receitas',
      url: `${GT_BASE}/transparencia/receitas/${id}${q}`,
      categoria: 'financeiro',
    },
    {
      titulo: 'Despesas detalhadas',
      url: `${GT_BASE}/transparencia/despesas/opcoes/${id}${q}`,
      categoria: 'financeiro',
    },
    {
      titulo: 'Despesas pagas',
      url: `${base}/consultarpagdesporc${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Ordem cronológica de pagamentos',
      url: `${base}/consultarpagdesporc${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Despesas por fornecedor',
      url: `${base}/consultardespesafornecedor${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Licitações, contratos e convênios',
      url: `${GT_BASE}/${id}${q}`,
      categoria: 'compras',
    },
    {
      titulo: 'Convênios',
      url: `${base}/consultarconvenio${q}`,
      categoria: 'compras',
    },
    {
      titulo: 'Obras e serviços de engenharia',
      url: `${GT_BASE}/transparencia/obras/${id}${q}`,
      categoria: 'obras',
    },
    {
      titulo: 'Projetos',
      url: `${GT_BASE}/transparencia/projetos/${id}${q}`,
      categoria: 'obras',
    },
    {
      titulo: 'Emendas parlamentares',
      url: `${GT_BASE}/acessoinfo/${id}/consultaremendas${q}`,
      categoria: 'emendas',
    },
    {
      titulo: 'Recursos federais (Lei 9.452/97)',
      url: `${base}/consultarrecursosfederais${q}`,
      categoria: 'financeiro',
    },
    {
      titulo: 'Dados abertos (exportar planilhas)',
      url: `${GT_BASE}/dadosabertos/${id}${q}`,
      categoria: 'dadosabertos',
    },
    {
      titulo: 'Folha — despesas por fornecedor (GT)',
      url: `${base}/consultardespesafornecedor${q}`,
      categoria: 'pessoal',
    },
  ];
}

async function scrapeGtResumo(http, exercicio = new Date().getFullYear()) {
  const exer = yearToExer(exercicio);
  const gtFolhaConsultaUrl =
    `${GT_BASE}/transparencia/${GT_PREFEITURA_ID}/consultardespesafornecedor?clean=false`;
  const basePayload = {
    receitaArrecadada: '',
    receitaPrevista: '',
    despesaPaga: '',
    percentualArrecadacao: '',
    periodoReferencia: '',
    dadosAtualizadosEm: '',
    consultadoEm: '',
    topFornecedores: [],
    folhaPorSetor: [],
    linksFinanceiros: buildGtFinanceLinks(),
    linksPortal: buildGtPortalLinks(),
    gtReceitasPainelUrl: `${GT_BASE}/transparencia/receitas/${GT_PREFEITURA_ID}?clean=false`,
    gtDespesasPainelUrl: `${GT_BASE}/transparencia/despesas/opcoes/${GT_PREFEITURA_ID}?clean=false`,
    gtDadosAbertosUrl: GT_DADOS_ABERTOS_URL,
    gtFolhaConsultaUrl,
    gtFonte: 'Governo Transparente',
    gtDisponivel: false,
  };

  try {
    const [receitasSettled, fornecedoresSettled, despesaSettled, portalSettled] = await Promise.allSettled([
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
      http.get(GT_DADOS_ABERTOS_URL, {
        headers: { Referer: GT_REFERER, Accept: 'text/html' },
        timeout: 30_000,
        validateStatus: (status) => status >= 200 && status < 400,
      }),
    ]);

    const receitasRows = receitasSettled.status === 'fulfilled' ? receitasSettled.value : [];
    const fornecedoresRows = fornecedoresSettled.status === 'fulfilled' ? fornecedoresSettled.value : [];
    const despesaHtml = despesaSettled.status === 'fulfilled' ? despesaSettled.value?.data : '';
    const portalHtml = portalSettled.status === 'fulfilled' ? portalSettled.value?.data : '';

    const { arrecadada, prevista } = sumReceitasTopLevel(receitasRows);
    const despesaPaga = parseDespesaTotalFromTitle(despesaHtml);
    const topFornecedores = mapTopFornecedores(fornecedoresRows);
    const folhaPorSetor = mapFolhaGtSetores(fornecedoresRows);
    const fornecedoresRowsRaw = Array.isArray(fornecedoresRows) ? fornecedoresRows : [];
    const dadosAtualizadosEm = parseDadosAtualizados(portalHtml)
      || parsePeriodoPortal(portalHtml).fim
      || parsePeriodoPortal(despesaHtml).fim;
    const periodoReferencia = buildPeriodoExercicio(exercicio, dadosAtualizadosEm);

    const hasReceita = arrecadada > 0;
    const hasDespesa = despesaPaga > 0;
    const hasFornecedores = topFornecedores.length > 0;
    const hasFolha = folhaPorSetor.length > 0;

    if (!hasReceita && !hasDespesa && !hasFornecedores && !hasFolha) {
      return basePayload;
    }

    return {
      receitaArrecadada: hasReceita ? formatBRL(arrecadada) : '',
      receitaPrevista: prevista > 0 ? formatBRL(prevista) : '',
      despesaPaga: hasDespesa ? formatBRL(despesaPaga) : '',
      percentualArrecadacao: calcPercentualArrecadacao(arrecadada, prevista),
      periodoReferencia,
      dadosAtualizadosEm,
      consultadoEm: new Date().toISOString(),
      topFornecedores,
      folhaPorSetor,
      fornecedoresRows: fornecedoresRowsRaw,
      linksFinanceiros: buildGtFinanceLinks(),
      linksPortal: buildGtPortalLinks(),
      gtReceitasPainelUrl: `${GT_BASE}/transparencia/receitas/${GT_PREFEITURA_ID}?clean=false`,
      gtDespesasPainelUrl: `${GT_BASE}/transparencia/despesas/opcoes/${GT_PREFEITURA_ID}?clean=false`,
      gtDadosAbertosUrl: GT_DADOS_ABERTOS_URL,
      gtFolhaConsultaUrl,
      gtFonte: 'Governo Transparente',
      gtDisponivel: hasReceita || hasDespesa || hasFornecedores,
    };
  } catch (err) {
    console.warn('[GT] resumo indisponível:', err.message);
    return basePayload;
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
  parsePeriodoPortal,
  parseDadosAtualizados,
  buildPeriodoExercicio,
  mapTopFornecedores,
  mapFolhaGtSetores,
  calcPercentualArrecadacao,
  buildGtFinanceLinks,
  buildGtPortalLinks,
  scrapeGtResumo,
};
