'use strict';

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';
const { parseBRLNumber, formatBRL } = require('./brl');
const { buildGtPortalLinks } = require('./scraper-governo-transparente');

function parseValorContrato(c) {
  if (typeof c.valorNumerico === 'number' && c.valorNumerico > 0) return c.valorNumerico;
  return parseBRLNumber(c.valor);
}

function buildResumoFinanceiro(
  contratos = [],
  licitacoes = [],
  exercicio = new Date().getFullYear(),
  gtResumo = null,
) {
  const totalValor = contratos.reduce((sum, c) => sum + parseValorContrato(c), 0);
  const contratosPeriodoReferencia =
    `Exercício ${exercicio} · ${contratos.length} contrato${contratos.length === 1 ? '' : 's'} no portal municipal`;
  const licitacoesAbertas = licitacoes.filter((l) => {
    const s = String(l.situacao || '').toLowerCase();
    return s.includes('abert') || s.includes('andamento') || s.includes('public') || s.includes('pregão');
  }).length;

  const gt = gtResumo && gtResumo.gtDisponivel ? gtResumo : null;
  let aviso =
    `Contratos: soma dos ${contratos.length} publicados no portal municipal (${contratosPeriodoReferencia}). `
    + 'Receitas e despesas executadas vêm do Governo Transparente quando disponíveis.';
  if (!gt) {
    aviso =
      `Soma dos contratos publicados no portal municipal (${contratosPeriodoReferencia}). `
      + 'Receitas e despesas detalhadas estão no Governo Transparente.';
  }

  return {
    totalContratosValor: formatBRL(totalValor),
    totalContratos: contratos.length,
    contratosPeriodoReferencia,
    licitacoesAbertas,
    exercicio,
    gtReceitasUrl: `${GT_BASE}/transparencia/receitas/${GT_PREFEITURA_ID}?clean=false`,
    gtDespesasUrl: `${GT_BASE}/transparencia/despesas/opcoes/${GT_PREFEITURA_ID}?clean=false`,
    receitaArrecadada: gt?.receitaArrecadada || '',
    receitaPrevista: gt?.receitaPrevista || '',
    despesaPaga: gt?.despesaPaga || '',
    percentualArrecadacao: gt?.percentualArrecadacao || '',
    periodoReferencia: gt?.periodoReferencia || '',
    dadosAtualizadosEm: gt?.dadosAtualizadosEm || '',
    consultadoEm: gt?.consultadoEm || '',
    topFornecedores: gt?.topFornecedores || [],
    linksFinanceiros: gt?.linksFinanceiros || [],
    gtReceitasPainelUrl: gt?.gtReceitasPainelUrl || `${GT_BASE}/transparencia/receitas/${GT_PREFEITURA_ID}?clean=false`,
    gtDespesasPainelUrl: gt?.gtDespesasPainelUrl || `${GT_BASE}/transparencia/despesas/opcoes/${GT_PREFEITURA_ID}?clean=false`,
    gtDadosAbertosUrl: gt?.gtDadosAbertosUrl || `${GT_BASE}/dadosabertos/${GT_PREFEITURA_ID}?clean=false`,
    gtFonte: gt?.gtFonte || '',
    gtDisponivel: Boolean(gt),
    linksPortal: (gtResumo?.linksPortal?.length ? gtResumo.linksPortal : buildGtPortalLinks()),
    remessa: gt?.remessa || null,
    ultimaRemessa: gt?.remessa?.dataUltimaRemessa || gt?.dadosAtualizadosEm || '',
    convenios: gt?.convenios || [],
    gtConveniosUrl: gt?.gtConveniosUrl || `${GT_BASE}/transparencia/${GT_PREFEITURA_ID}/consultarconvenio?clean=false`,
    receitasPorRubrica: gt?.receitasPorRubrica || [],
    aviso,
  };
}

module.exports = { buildResumoFinanceiro };
