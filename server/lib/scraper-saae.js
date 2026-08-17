'use strict';

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';
const { parseBRLNumber, formatBRL } = require('./brl');

const SAAE_TEXT = /saae|044\s*-|servi[cç]o aut[oô]nomo de [aá]gua|[aá]gua e esgoto|abastecimento de [aá]gua|cagece|sistema de [aá]gua|oxig[eê]nio.*saae|saae.*oxig[eê]nio/i;
const FOLHA_TEXT = /folha de pagamento/i;

function matchesSaae(text) {
  return SAAE_TEXT.test(String(text || ''));
}

function parseValorContrato(c) {
  if (typeof c.valorNumerico === 'number' && c.valorNumerico > 0) return c.valorNumerico;
  return parseBRLNumber(c.valor);
}

function filterSaaeContratos(contratos = []) {
  return contratos.filter((c) => matchesSaae(JSON.stringify(c)));
}

function filterSaaeLicitacoes(licitacoes = []) {
  return licitacoes.filter((l) => matchesSaae(JSON.stringify(l)));
}

function mapLinhasFinanceiras(fornecedoresRows = []) {
  if (!Array.isArray(fornecedoresRows)) return [];
  return fornecedoresRows
    .filter((row) => row && matchesSaae(row.nome))
    .map((row) => {
      const valor = Number(row.valor) || 0;
      const folha = FOLHA_TEXT.test(String(row.nome || ''));
      return {
        descricao: String(row.nome || '').trim(),
        valor: formatBRL(valor),
        valorNumerico: valor,
        tipo: folha ? 'folha' : 'fornecedor',
      };
    })
    .filter((row) => row.valorNumerico > 0)
    .sort((a, b) => b.valorNumerico - a.valorNumerico)
    .map(({ valorNumerico, ...rest }) => rest);
}

function buildSaaeLinks(exercicio) {
  const id = GT_PREFEITURA_ID;
  const base = `${GT_BASE}/transparencia/${id}`;
  const q = '?clean=false';
  return [
    {
      titulo: 'Despesas pagas — unidade SAAE (GT)',
      url: `${base}/consultarpagdesporc${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Despesas por fornecedor (GT)',
      url: `${base}/consultardespesafornecedor${q}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Receitas — Prefeitura/SAAE (GT)',
      url: `${GT_BASE}/transparencia/receitas/${id}${q}`,
      categoria: 'receita',
    },
    {
      titulo: 'Pagamentos por órgão — Prefeitura',
      url: `https://www.caninde.ce.gov.br/lcpagamentos.php?ANO=${exercicio}`,
      categoria: 'despesa',
    },
    {
      titulo: 'Portal Governo Transparente',
      url: `${GT_BASE}/${id}${q}`,
      categoria: 'portal',
    },
    {
      titulo: 'Dados abertos (exportar)',
      url: `${GT_BASE}/dadosabertos/${id}${q}`,
      categoria: 'dadosabertos',
    },
  ];
}

function buildSaaeResumo(
  exercicio,
  contratos = [],
  licitacoes = [],
  fornecedoresRows = [],
  meta = {},
) {
  const contratosSaae = filterSaaeContratos(contratos);
  const licitacoesSaae = filterSaaeLicitacoes(licitacoes);
  const linhasFinanceiras = mapLinhasFinanceiras(fornecedoresRows);

  const folhaTotal = linhasFinanceiras
    .filter((l) => l.tipo === 'folha')
    .reduce((sum, l) => sum + parseBRLNumber(l.valor), 0);
  const outrasDespesas = linhasFinanceiras
    .filter((l) => l.tipo !== 'folha')
    .reduce((sum, l) => sum + parseBRLNumber(l.valor), 0);
  const contratosTotal = contratosSaae.reduce((sum, c) => sum + parseValorContrato(c), 0);

  const hasFinanceiro = folhaTotal > 0 || outrasDespesas > 0;
  const hasCompras = contratosSaae.length > 0 || licitacoesSaae.length > 0;

  return {
    exercicio,
    titulo: 'SAAE — Serviço Autônomo de Água e Esgoto',
    codigoOrgao: '044',
    folhaPagamento: folhaTotal > 0 ? formatBRL(folhaTotal) : '',
    totalDespesasGt: outrasDespesas > 0 ? formatBRL(outrasDespesas) : '',
    totalContratos: contratosTotal > 0 ? formatBRL(contratosTotal) : '',
    quantidadeContratos: contratosSaae.length,
    quantidadeLicitacoes: licitacoesSaae.length,
    linhasFinanceiras,
    contratos: contratosSaae.slice(0, 20),
    licitacoes: licitacoesSaae.slice(0, 20),
    links: buildSaaeLinks(exercicio),
    fonte: 'Governo Transparente + Portal Municipal',
    aviso:
      'Órgão 044 — Serviço Autônomo de Água e Esgoto de Canindé. '
      + 'Valores agregados do Governo Transparente (exercício selecionado na Prefeitura). '
      + 'Contratos e licitações filtrados por objeto relacionado a água, esgoto ou SAAE.',
    dadosAtualizadosEm: meta.dadosAtualizadosEm || '',
    disponivel: hasFinanceiro || hasCompras || linhasFinanceiras.length > 0,
  };
}

module.exports = {
  SAAE_TEXT,
  matchesSaae,
  filterSaaeContratos,
  filterSaaeLicitacoes,
  mapLinhasFinanceiras,
  buildSaaeLinks,
  buildSaaeResumo,
};
