'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { enrichSecretarias, matchByText, buildMatchers } = require('../lib/secretaria-enrich');

describe('secretaria-enrich', () => {
  const secretarias = [
    { id: '9', nome: 'Secretaria Municipal da Educação', secretario: 'Géssica', contato: {} },
    { id: '3', nome: 'Secretaria de Segurança Pública e Trânsito', secretario: 'Francisco', contato: {} },
  ];

  const contratos = [
    {
      numero: '001/2026',
      objeto: 'Transporte escolar',
      valor: 'R$ 10.000,00',
      valorNumerico: 10000,
      secretaria: 'Secretaria Municipal da Educação',
      url: 'https://example/c1',
      vigenciaFim: '31/12/2028',
    },
    {
      numero: '002/2026',
      objeto: 'Segurança viária',
      valor: 'R$ 5.000,00',
      valorNumerico: 5000,
      secretaria: 'Secretaria de Segurança Pública e Trânsito',
      url: 'https://example/c2',
    },
  ];

  const licitacoes = [
    {
      numero: '03/2026',
      objeto: 'CONTRATAÇÃO DE EMPRESA PARA A SECRETARIA MUNICIPAL DE EDUCAÇÃO',
      situacao: 'Em andamento',
      url: 'https://example/l1',
    },
  ];

  it('vincula contratos e licitações por secretaria', () => {
    const out = enrichSecretarias(secretarias, contratos, licitacoes);
    const edu = out.find((s) => s.id === '9');
    const seg = out.find((s) => s.id === '3');
    assert.equal(edu.contratos.length, 1);
    assert.equal(edu.licitacoes.length, 1);
    assert.equal(edu.resumoFinanceiro.totalContratos, 1);
    assert.equal(edu.resumoFinanceiro.totalLicitacoes, 1);
    assert.ok(edu.resumoFinanceiro.totalGastos.includes('10.000'));
    assert.equal(seg.contratos.length, 1);
    assert.ok(edu.projetosAndamento.length >= 1);
  });

  it('matchByText encontra alias da secretaria no objeto', () => {
    const matchers = buildMatchers(secretarias);
    const id = matchByText('SERVICOS PARA EDUCACAO MUNICIPAL', matchers);
    assert.equal(id, '9');
  });
});
