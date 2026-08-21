'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  mergeFolhaPagamento,
  extractCompetenciaAno,
  parseCompetenciaSortKey,
  sortCompetenciasDesc,
  resolveReferenciaFolha,
} = require('../lib/folha-gt-merge');
const { mapFolhaGtSetores, buildGtPortalLinks } = require('../lib/scraper-governo-transparente');

describe('folha-gt-merge', () => {
  it('extractCompetenciaAno lê ano da competência mais recente', () => {
    assert.equal(
      extractCompetenciaAno([
        { competencia: 'JANEIRO/2024' },
        { competencia: 'DEZEMBRO/2025' },
      ]),
      '2025',
    );
  });

  it('sortCompetenciasDesc ordena competências do portal', () => {
    const sorted = sortCompetenciasDesc([
      { competencia: 'JANEIRO/2024' },
      { competencia: '01/2026' },
      { competencia: 'MARCO/2025' },
    ]);
    assert.equal(sorted[0].competencia, '01/2026');
    assert.equal(parseCompetenciaSortKey('01/2026'), 202601);
  });

  it('resolveReferenciaFolha prefere competência SST mais recente', () => {
    const ref = resolveReferenciaFolha(
      { disponivel: true, competencia: '01/2026' },
      { competencias: [{ competencia: 'JANEIRO/2024' }] },
      2024,
    );
    assert.equal(ref.referenciaExercicio, 2026);
    assert.equal(ref.referenciaCompetencia, '01/2026');
  });

  it('mergeFolhaPagamento prefere GT quando totais são maiores', () => {
    const merged = mergeFolhaPagamento(
      {
        competencias: [{ competencia: 'JANEIRO/2024' }],
        porSetor: [{ secretaria: 'Portal', totalPagoNumerico: 1000, quantidadePagamentos: 1 }],
        avisoPrivacidade: 'LGPD',
        fonteUrl: 'https://example.com/folha',
        fontePagamentosUrl: 'https://example.com/pag',
      },
      [
        { secretaria: 'SAUDE', totalPagoNumerico: 5000000, quantidadePagamentos: 3, totalPago: 'R$ 5.000.000,00' },
      ],
      2026,
      { gtFolhaConsultaUrl: 'https://example.com/gt-folha' },
      null,
    );

    assert.equal(merged.fontePorSetor, 'governo_transparente');
    assert.equal(merged.exercicio, 2026);
    assert.equal(merged.referenciaExercicio, 2026);
    assert.equal(merged.porSetor.length, 1);
    assert.equal(merged.porSetor[0].secretaria, 'SAUDE');
    assert.match(merged.avisoDados, /2024/);
    assert.match(merged.avisoDados, /Governo Transparente/);
    assert.equal(merged.gtConsultaUrl, 'https://example.com/gt-folha');
  });
});

describe('mapFolhaGtSetores', () => {
  it('agrega linhas FOLHA DE PAGAMENTO do GT', () => {
    const setores = mapFolhaGtSetores([
      { nome: 'FOLHA DE PAGAMENTO - SMS', valor: 1000 },
      { nome: 'FOLHA DE PAGAMENTO - SMS', valor: 500 },
      { nome: 'EMPRESA ABC', valor: 9999 },
    ]);
    assert.equal(setores.length, 1);
    assert.equal(setores[0].secretaria, 'SMS');
    assert.equal(setores[0].totalPagoNumerico, 1500);
    assert.equal(setores[0].quantidadePagamentos, 2);
  });
});

describe('buildGtPortalLinks', () => {
  it('inclui obras, convênios e dados abertos', () => {
    const links = buildGtPortalLinks();
    assert.ok(links.some((l) => l.categoria === 'obras'));
    assert.ok(links.some((l) => l.titulo.includes('Convênios')));
    assert.ok(links.some((l) => l.categoria === 'dadosabertos'));
    assert.ok(links.some((l) => l.categoria === 'portal'));
  });
});
