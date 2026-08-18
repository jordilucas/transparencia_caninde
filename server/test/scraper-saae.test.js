'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  matchesSaae,
  filterSaaeContratos,
  mapLinhasFinanceiras,
  buildSaaeResumo,
} = require('../lib/scraper-saae');

describe('scraper-saae', () => {
  it('matchesSaae detecta SAAE e água/esgoto', () => {
    assert.equal(matchesSaae('FOLHA DE PAGAMENTO-SAAE/ADMINISTRACAO'), true);
    assert.equal(matchesSaae('SERVIÇO AUTONOMO DE AGUA E ESGOTO - SAAE'), true);
    assert.equal(matchesSaae('CONTRATAÇÃO DE OXIGÊNIO PARA SAAE'), true);
    assert.equal(matchesSaae('FORNECEDOR GENERICO'), false);
  });

  it('filterSaaeContratos filtra por objeto', () => {
    const contratos = [
      { numero: '1', objeto: 'OXIGENIO PARA O SAAE', valor: 'R$ 100,00' },
      { numero: '2', objeto: 'PAPEL A4', valor: 'R$ 50,00' },
    ];
    assert.equal(filterSaaeContratos(contratos).length, 1);
  });

  it('mapLinhasFinanceiras separa folha e fornecedor', () => {
    const linhas = mapLinhasFinanceiras([
      { nome: 'FOLHA DE PAGAMENTO-SAAE', valor: 1000 },
      { nome: 'CAGECE-COMPANHIA DE ÁGUA E ESGOTO DO CEARÁ', valor: 500 },
    ]);
    assert.equal(linhas.length, 2);
    assert.equal(linhas[0].tipo, 'folha');
    assert.equal(linhas[1].tipo, 'fornecedor');
  });

  it('buildSaaeResumo agrega totais e listas', () => {
    const resumo = buildSaaeResumo(
      2026,
      [{ numero: 'C1', objeto: 'SAAE oxigenio', valor: 'R$ 10.000,00', valorNumerico: 10000 }],
      [{ numero: 'L1', objeto: 'água e esgoto', situacao: 'Aberta' }],
      [{ nome: 'FOLHA DE PAGAMENTO-SAAE', valor: 3138789.15 }],
      {
        dadosAtualizadosEm: '17/08/2026',
        pagamentosSaae: [{ data: '10/08/2026', credor: 'CAGECE', valor: 'R$ 500,00' }],
        totalPagamentosSaae: 'R$ 500,00',
        quantidadePagamentosSaae: 1,
        saaeOrgaoNome: '044 - SAAE',
      },
    );
    assert.equal(resumo.exercicio, 2026);
    assert.equal(resumo.codigoOrgao, '044');
    assert.ok(resumo.disponivel);
    assert.equal(resumo.quantidadeContratos, 1);
    assert.equal(resumo.quantidadeLicitacoes, 1);
    assert.ok(resumo.folhaPagamento.includes('3.138.789'));
    assert.equal(resumo.pagamentos.length, 1);
    assert.ok(resumo.totalPagamentosOrgao.includes('500'));
    assert.equal(resumo.orgaoNome, '044 - SAAE');
    assert.ok(resumo.links.length >= 4);
  });
});
