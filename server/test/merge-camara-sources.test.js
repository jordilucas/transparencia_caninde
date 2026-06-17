'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { mergeCamaraSources, mergeParlamentar } = require('../lib/merge-camara-sources');

describe('merge-camara-sources', () => {
  it('prefere WP para sessões mais recentes e HTML para foto/partido', () => {
    const wp = {
      parlamentares: [{
        slug: 'maria-taylana',
        nome: 'Maria Taylana',
        nomeCompleto: 'Maria Taylana',
        cargo: 'Vereador(a)',
        vinculo: 'Vereador(a) em Exercício',
        legislatura: '2025 - 2028',
        modifiedAt: '2026-04-16T08:20:00',
        partido: '',
        foto: '',
        totalMaterias: 0,
        totalSessoes: 0,
      }],
      sessoes: [{
        slug: 'sessao-1',
        titulo: 'Sessão WP',
        data: '16/04/2026',
        url: 'https://example/sessao-1/',
        modifiedAt: '2026-04-16T08:20:00',
      }],
      fontesUtilizadas: ['wp-rest'],
    };
    const html = {
      parlamentares: [{
        slug: 'maria-taylana',
        nome: 'Taylana',
        nomeCompleto: 'Maria Taylana Queiroz Martins',
        partido: 'PSB',
        cargo: 'Vereador(a)',
        foto: 'https://example/foto.jpg',
        totalMaterias: 3,
        totalSessoes: 2,
        modifiedAt: '',
      }],
      sessoes: [{
        slug: 'sessao-1',
        titulo: 'Sessão HTML',
        data: '01/01/2025',
        url: 'https://example/sessao-1/',
        modifiedAt: '',
      }],
      fontesUtilizadas: ['html'],
    };

    const merged = mergeCamaraSources(wp, html);
    assert.equal(merged.parlamentares.length, 1);
    assert.equal(merged.parlamentares[0].partido, 'PSB');
    assert.equal(merged.parlamentares[0].legislatura, '2025 - 2028');
    assert.equal(merged.parlamentares[0].totalMaterias, 3);
    assert.equal(merged.sessoes[0].titulo, 'Sessão WP');
    assert.deepEqual(merged.fontesUtilizadas.sort(), ['html', 'wp-rest']);
  });

  it('mergeParlamentar combina campos complementares', () => {
    const merged = mergeParlamentar(
      { slug: 'a', nome: 'A', modifiedAt: '2026-01-01T00:00:00', totalMaterias: 1 },
      { slug: 'a', nome: 'A', partido: 'PT', totalSessoes: 2 },
    );
    assert.equal(merged.partido, 'PT');
    assert.equal(merged.totalMaterias, 1);
    assert.equal(merged.totalSessoes, 2);
  });
});
