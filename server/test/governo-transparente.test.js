'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  yearToExer,
  sumReceitasTopLevel,
  parseDespesaTotalFromTitle,
  parsePeriodoPortal,
  parseDadosAtualizados,
  buildPeriodoExercicio,
  mapTopFornecedores,
  calcPercentualArrecadacao,
  buildGtFinanceLinks,
} = require('../lib/scraper-governo-transparente');
const { buildResumoFinanceiro } = require('../lib/finance-summary');

describe('scraper-governo-transparente', () => {
  it('yearToExer converte exercício', () => {
    assert.equal(yearToExer(2026), 17);
    assert.equal(yearToExer(2024), 15);
  });

  it('sumReceitasTopLevel soma categorias de primeiro nível', () => {
    const rows = [
      { codigo: '001.0.0.0.00.0.0.00.00.00', saldo: 100, previsao: 200 },
      { codigo: '002.0.0.0.00.0.0.00.00.00', saldo: 50, previsao: 80 },
      { codigo: '001.1.1.0.00.0.0.00.00.00', saldo: 999, previsao: 999 },
    ];
    const total = sumReceitasTopLevel(rows);
    assert.equal(total.arrecadada, 150);
    assert.equal(total.prevista, 280);
  });

  it('parseDespesaTotalFromTitle extrai valor do título HTML', () => {
    const html = '<title>Total das despesas: R$ 272.622.229,70.</title>';
    assert.equal(parseDespesaTotalFromTitle(html), 272622229.7);
  });

  it('mapTopFornecedores exclui folha de pagamento', () => {
    const rows = [
      { nome: 'FOLHA DE PAGAMENTO - SAUDE', cpfcnpj: '1', valor: 1000 },
      { nome: 'EMPRESA ABC LTDA', cpfcnpj: '2', valor: 500 },
      { nome: 'EMPRESA XYZ', cpfcnpj: '3', valor: 800 },
    ];
    const top = mapTopFornecedores(rows, 2);
    assert.equal(top.length, 2);
    assert.equal(top[0].nome, 'EMPRESA XYZ');
    assert.equal(top[1].nome, 'EMPRESA ABC LTDA');
  });

  it('calcPercentualArrecadacao formata percentual', () => {
    assert.equal(calcPercentualArrecadacao(307475962, 520000000), '59,1%');
  });

  it('buildGtFinanceLinks inclui receitas e despesas', () => {
    const links = buildGtFinanceLinks();
    assert.ok(links.some((l) => l.titulo.includes('receitas')));
    assert.ok(links.some((l) => l.categoria === 'receita'));
    assert.ok(links.some((l) => l.categoria === 'despesa'));
  });

  it('parseDadosAtualizados extrai data do portal', () => {
    const html = '<p>Dados atualizados 17/08/2026. Para consultar</p>';
    assert.equal(parseDadosAtualizados(html), '17/08/2026');
  });

  it('buildPeriodoExercicio monta intervalo do ano corrente', () => {
    assert.equal(buildPeriodoExercicio(2026, '17/08/2026'), '01/01/2026 a 17/08/2026');
    assert.equal(buildPeriodoExercicio(2024, '17/08/2026'), 'Exercício 2024');
  });

  it('buildResumoFinanceiro inclui datas GT', () => {
    const resumo = buildResumoFinanceiro([], [], 2026, {
      gtDisponivel: true,
      periodoReferencia: '01/01/2026 a 17/08/2026',
      dadosAtualizadosEm: '17/08/2026',
      consultadoEm: '2026-08-17T19:00:00Z',
    });
    assert.equal(resumo.periodoReferencia, '01/01/2026 a 17/08/2026');
    assert.equal(resumo.dadosAtualizadosEm, '17/08/2026');
    assert.ok(resumo.consultadoEm.includes('2026'));
  });

  it('buildResumoFinanceiro inclui dados GT quando disponíveis', () => {
    const resumo = buildResumoFinanceiro([], [], 2026, {
      gtDisponivel: true,
      receitaArrecadada: 'R$ 100,00',
      despesaPaga: 'R$ 80,00',
      receitaPrevista: 'R$ 120,00',
      topFornecedores: [{ nome: 'Empresa', cnpj: '00.000.000/0001-00', valor: 'R$ 50,00' }],
      gtDadosAbertosUrl: 'https://example.com/dadosabertos',
      gtFonte: 'Governo Transparente',
    });
    assert.equal(resumo.gtDisponivel, true);
    assert.equal(resumo.receitaArrecadada, 'R$ 100,00');
    assert.equal(resumo.topFornecedores.length, 1);
    assert.ok(resumo.gtDadosAbertosUrl.includes('dadosabertos'));
  });
});
