'use strict';

const GT_PREFEITURA_ID = '11979490';
const GT_BASE = 'https://www.governotransparente.com.br';

function formatBRL(value) {
  if (value == null || Number.isNaN(value)) return '';
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function parseValorContrato(c) {
  if (typeof c.valorNumerico === 'number' && c.valorNumerico > 0) return c.valorNumerico;
  const raw = String(c.valor || '').replace(/[^\d,.-]/g, '');
  if (!raw) return 0;
  const n = parseFloat(raw.replace(/\./g, '').replace(',', '.'));
  return Number.isNaN(n) ? 0 : n;
}

function buildResumoFinanceiro(contratos = [], licitacoes = [], exercicio = new Date().getFullYear()) {
  const totalValor = contratos.reduce((sum, c) => sum + parseValorContrato(c), 0);
  const licitacoesAbertas = licitacoes.filter((l) => {
    const s = String(l.situacao || '').toLowerCase();
    return s.includes('abert') || s.includes('andamento') || s.includes('public') || s.includes('pregão');
  }).length;

  return {
    totalContratosValor: formatBRL(totalValor),
    totalContratos: contratos.length,
    licitacoesAbertas,
    exercicio,
    gtReceitasUrl: `${GT_BASE}/transparencia/receitas/${GT_PREFEITURA_ID}?clean=false`,
    gtDespesasUrl: `${GT_BASE}/transparencia/despesas/opcoes/${GT_PREFEITURA_ID}?clean=false`,
    aviso:
      'Valores somados dos contratos publicados no portal municipal (dados abertos). '
      + 'Receitas e despesas detalhadas estão no Governo Transparente.',
  };
}

module.exports = { buildResumoFinanceiro };
