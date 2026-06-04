'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  buildCamaraPayload,
  buildPrefeituraPayload,
  basicCamaraShell,
} = require('../lib/scrape-result');

describe('scrape-result', () => {
  it('prefeitura vazia retorna erro sem listas inventadas', () => {
    const p = buildPrefeituraPayload({});
    assert.equal(p.contratos.length, 0);
    assert.match(p.error, /Prefeitura de Canindé/);
  });

  it('câmara com vereadores reais não inventa mesa', () => {
    const c = buildCamaraPayload({
      parlamentares: [{ nome: 'Karlinda Coelho', partido: 'REP', cargo: 'Presidente', foto: '' }],
      mesaDiretora: [],
    });
    assert.equal(c.parlamentares.length, 1);
    assert.equal(c.mesaDiretora.length, 0);
    assert.match(c.error, /mesa diretora/);
  });

  it('shell básico só traz metadados e erro', () => {
    const s = basicCamaraShell('timeout');
    assert.equal(s.parlamentares.length, 0);
    assert.match(s.error, /timeout/);
    assert.equal(s.municipio, 'Canindé');
  });
});
