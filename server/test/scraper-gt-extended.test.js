'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  parseRemessa,
  mapReceitasTopRubricas,
  findSaaeOrgaoId,
  mapPagamentosRows,
  sumPagamentos,
  parseConveniosHtml,
  scrapeGtExtended,
} = require('../lib/scraper-gt-extended');
const { buildResumoFinanceiro } = require('../lib/finance-summary');

describe('scraper-gt-extended', () => {
  it('parseRemessa extrai datas da API', () => {
    const remessa = parseRemessa({
      dataUltimaRemessa: '15/08/2026',
      dataPrimeiroMovimento: '02/01/2026',
      dataUltimoMovimento: '14/08/2026',
    });
    assert.equal(remessa.dataUltimaRemessa, '15/08/2026');
    assert.equal(remessa.dataUltimoMovimento, '14/08/2026');
  });

  it('mapReceitasTopRubricas filtra nível principal e ordena', () => {
    const rubricas = mapReceitasTopRubricas([
      { codigo: '001.0.0.0.00.0.0.00.00.00', especificacao: 'Receitas correntes', arrecadado: 500 },
      { codigo: '002.0.0.0.00.0.0.00.00.00', especificacao: 'Receitas de capital', arrecadado: 200 },
      { codigo: '001.1.1.0.00.0.0.00.00.00', especificacao: 'Detalhe', arrecadado: 9999 },
    ], 2);
    assert.equal(rubricas.length, 2);
    assert.equal(rubricas[0].nome, 'Receitas correntes');
    assert.ok(rubricas[0].valorFormatado.includes('500'));
    assert.equal(rubricas[0].valorNumerico, 500);
  });

  it('findSaaeOrgaoId identifica órgão SAAE', () => {
    const org = findSaaeOrgaoId([
      { id: 10, nome: '001 - PREFEITURA' },
      { id: 44, nome: '044 - SERVIÇO AUTONOMO DE AGUA E ESGOTO' },
    ]);
    assert.equal(org.id, '44');
    assert.match(org.nome, /SAAE|AGUA/i);
  });

  it('mapPagamentosRows ignora anulações e formata valores', () => {
    const rows = mapPagamentosRows([
      { dataMovimento: '10/08/2026', credor: 'FORNECEDOR A', valor: 1500, registro: 'Pagamento' },
      { dataMovimento: '11/08/2026', credor: 'ANULADO', valor: 500, registro: 'Anulação' },
    ]);
    assert.equal(rows.length, 1);
    assert.equal(rows[0].credor, 'FORNECEDOR A');
    assert.ok(rows[0].valor.includes('1.500'));
  });

  it('sumPagamentos soma movimentações válidas', () => {
    const total = sumPagamentos([
      { valor: 100, registro: 'Pagamento' },
      { valor: 50, registro: 'Anulação de pagamento' },
      { valor: 200, registro: 'Pagamento' },
    ]);
    assert.equal(total, 300);
  });

  it('parseConveniosHtml extrai linhas de tabela', () => {
    const html = `
      <table>
        <tr><th>Número</th><th>Objeto</th><th>Parceiro</th><th>Valor</th><th>Situação</th></tr>
        <tr><td>001/2026</td><td>Obra de saneamento</td><td>Estado do CE</td><td>R$ 1.000.000,00</td><td>Vigente</td></tr>
      </table>
    `;
    const convenios = parseConveniosHtml(html);
    assert.equal(convenios.length, 1);
    assert.equal(convenios[0].numero, '001/2026');
    assert.match(convenios[0].valor, /1\.000\.000/);
  });

  it('buildResumoFinanceiro repassa convênios, remessa e rubricas', () => {
    const resumo = buildResumoFinanceiro([], [], 2026, {
      gtDisponivel: true,
      remessa: { dataUltimaRemessa: '15/08/2026' },
      convenios: [{ numero: 'C1', objeto: 'Teste', valor: 'R$ 1,00' }],
      receitasPorRubrica: [{ nome: 'Correntes', valorFormatado: 'R$ 100,00', valorNumerico: 100 }],
      gtConveniosUrl: 'https://example.com/convenios',
    });
    assert.equal(resumo.ultimaRemessa, '15/08/2026');
    assert.equal(resumo.convenios.length, 1);
    assert.equal(resumo.receitasPorRubrica.length, 1);
    assert.ok(resumo.gtConveniosUrl.includes('convenios'));
  });

  it('scrapeGtExtended não quebra com lista de órgãos vazia', async () => {
    const http = {
      get: async () => ({ data: [] }),
    };
    const result = await scrapeGtExtended(http, 2026, [
      { codigo: '001.0.0.0.00.0.0.00.00.00', especificacao: 'Receitas correntes', arrecadado: 100 },
    ]);
    assert.ok(Array.isArray(result.convenios));
    assert.equal(result.pagamentosSaae.length, 0);
    assert.equal(result.receitasPorRubrica.length, 1);
  });
});
