'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { parseBRLNumber, formatBRL } = require('../lib/brl');
const { buildResumoFinanceiro } = require('../lib/finance-summary');
const { mapContratos } = require('../lib/scraper-prefeitura-dadosabertos');

describe('brl', () => {
  it('parseBRLNumber aceita número JSON com decimal', () => {
    assert.equal(parseBRLNumber(2004257.55), 2004257.55);
    assert.equal(parseBRLNumber(61105.5), 61105.5);
  });

  it('parseBRLNumber aceita formato brasileiro com vírgula', () => {
    assert.equal(parseBRLNumber('2.004.257,55'), 2004257.55);
    assert.equal(parseBRLNumber('61.105,50'), 61105.5);
  });

  it('parseBRLNumber aceita string decimal americana', () => {
    assert.equal(parseBRLNumber('2004257.55'), 2004257.55);
  });

  it('parseBRLNumber ignora data colada antes do valor', () => {
    assert.equal(parseBRLNumber('24/07/2026 R$ 3.479.365,46'), 3479365.46);
    assert.equal(parseBRLNumber('12/06/202665.000,00'), 65000);
  });

  it('buildResumoFinanceiro não infla totais de contratos JSON', () => {
    const contratos = mapContratos([
      { Id: 1, NumeroContrato: '2025.12.17.04', ValorGlobal: 2004257.55, Objeto: 'Teste' },
      { Id: 2, NumeroContrato: '02.001', ValorGlobal: 2603016.05, Objeto: 'Teste 2' },
    ]);
    const resumo = buildResumoFinanceiro(contratos, [], 2025);
    assert.ok(resumo.totalContratosValor.includes('4.607.273'));
    assert.match(resumo.contratosPeriodoReferencia, /Exercício 2025/);
    assert.match(resumo.contratosPeriodoReferencia, /2 contratos/);
  });
});
