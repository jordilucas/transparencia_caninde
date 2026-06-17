'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  mapVereadorFromWp,
  mapSessaoFromWp,
  mapMateriaFromWp,
  formatWpDate,
} = require('../lib/scraper-camara-wp');

describe('scraper-camara-wp', () => {
  it('formatWpDate converte ISO para BR', () => {
    assert.equal(formatWpDate('2026-04-16T08:20:00'), '16/04/2026');
  });

  it('mapVereadorFromWp resolve taxonomias', () => {
    const item = {
      slug: 'maria-taylana',
      title: { rendered: 'Maria Taylana' },
      link: 'https://www.cmcaninde.ce.gov.br/vereadores/maria-taylana/',
      cargo: [91],
      vinculo: [58],
      ano_legislatura: [46],
      class_list: ['ano_legislatura-2025-2028', 'cargo-vereadora'],
      modified: '2026-04-16T08:20:00',
    };
    const taxMaps = {
      cargo: new Map([[91, 'Vereador(a)']]),
      vinculo: new Map([[58, 'Vereador(a) em Exercício']]),
      ano_legislatura: new Map([[46, '2025-2028']]),
    };
    const v = mapVereadorFromWp(item, taxMaps);
    assert.equal(v.nome, 'Maria Taylana');
    assert.equal(v.cargo, 'Vereador(a)');
    assert.equal(v.vinculo, 'Vereador(a) em Exercício');
    assert.equal(v.legislatura, '2025 - 2028');
  });

  it('mapSessaoFromWp inclui slug e data', () => {
    const s = mapSessaoFromWp({
      title: { rendered: '1ª Sessão Extraordinária' },
      slug: '1a-sessao-extraordinaria',
      link: 'https://example/sessao/1/',
      date: '2025-03-14T13:29:56',
      modified: '2026-04-16T08:20:18',
    });
    assert.equal(s.slug, '1a-sessao-extraordinaria');
    assert.equal(s.data, '14/03/2025');
  });

  it('mapMateriaFromWp infere tipo', () => {
    const m = mapMateriaFromWp({
      title: { rendered: 'Requerimentos 121 ao 147' },
      slug: 'requerimentos-121-ao-147',
      link: 'https://example/materia/r/',
      modified: '2026-04-16T08:21:00',
      tipo_materia: [],
    }, new Map());
    assert.equal(m.tipo, 'Requerimento');
  });
});
