'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { buildPrefeituraCharts, buildCamaraCharts } = require('../lib/charts');

describe('charts', () => {
  it('agrega licitações por situação', () => {
    const g = buildPrefeituraCharts({
      licitacoes: [
        { situacao: 'Aberta' },
        { situacao: 'Aberta' },
        { situacao: 'Homologada' },
      ],
      contratos: [],
      secretarias: [],
    });
    const lic = g.prefeitura.find((s) => s.titulo.includes('situação'));
    assert.ok(lic);
    assert.ok(lic.valores.includes(2));
  });

  it('agrega matérias por tipo na câmara', () => {
    const g = buildCamaraCharts({
      materias: [
        { tipo: 'Requerimento' },
        { tipo: 'Projeto de Lei' },
        { tipo: 'Requerimento' },
      ],
      parlamentares: [{ nome: 'A' }],
      sessoes: [],
    });
    const mat = g.camara.find((s) => s.titulo.includes('tipo'));
    assert.ok(mat);
    assert.equal(mat.valores.reduce((a, b) => a + b, 0), 3);
  });
});
