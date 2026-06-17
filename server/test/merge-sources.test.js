'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  mergeContratos,
  mergePrefeituraSources,
  parseBrazilianDate,
} = require('../lib/merge-sources');

describe('merge-sources', () => {
  it('parseBrazilianDate interpreta dd/mm/yyyy', () => {
    const t = parseBrazilianDate('15/03/2025');
    assert.ok(t > parseBrazilianDate('01/01/2025'));
  });

  it('mergeContratos mantém registro com data mais recente', () => {
    const json = [{
      numero: '001/2025',
      objeto: 'Objeto JSON',
      data: '01/01/2025',
      valor: 'R$ 100',
      fonteOrigem: 'json',
    }];
    const html = [{
      numero: '001/2025',
      objeto: 'Objeto HTML',
      data: '20/06/2025',
      valor: '',
      fonteOrigem: 'html',
    }];
    const { items } = mergeContratos(json, html);
    assert.equal(items.length, 1);
    assert.equal(items[0].data, '20/06/2025');
    assert.equal(items[0].valor, 'R$ 100');
    assert.match(items[0].objeto, /HTML|JSON/);
  });

  it('mergeContratos une listas distintas ordenadas por data', () => {
    const json = [{ numero: 'A', data: '10/01/2025', fonteOrigem: 'json' }];
    const html = [{ numero: 'B', data: '15/06/2025', fonteOrigem: 'html' }];
    const { items } = mergeContratos(json, html);
    assert.equal(items.length, 2);
    assert.equal(items[0].numero, 'B');
  });

  it('mergePrefeituraSources combina publicações JSON e diários HTML', () => {
    const merged = mergePrefeituraSources(
      {
        publicacoes: [{ id: '1', titulo: 'Antigo', data: '01/01/2025', fonteOrigem: 'json' }],
        contratos: [],
        licitacoes: [],
        secretarias: [],
      },
      {
        diarios: ['Diário Oficial — 30/06/2025 — Portaria 99'],
        contratos: [],
        licitacoes: [],
        secretarias: [],
      },
    );
    assert.ok(merged.publicacoes.length >= 2);
    assert.ok(merged.fontesUtilizadas.includes('json'));
    assert.ok(merged.fontesUtilizadas.includes('html'));
    assert.equal(merged.publicacoes[0].data, '30/06/2025');
  });
});
