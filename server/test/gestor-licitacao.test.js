'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const cheerio = require('cheerio');
const { parseGestoresAtual } = require('../lib/gestor-html');
const { filterValidLicitacoes } = require('../lib/licitacao-html');

describe('gestor-html', () => {
  it('extrai prefeito e vice atuais dos cards', () => {
    const html = `
      <div class="row">
        <div class="titlepre">
          <p><strong>Francisco Jardel Sousa Pinho</strong></p>
          <p>Prefeito(a)</p>
        </div>
      </div>
      <div class="row">
        <div class="titlepre">
          <p><strong>Antonio Ilomar Vasconcelos Cruz</strong></p>
          <p>Vice-Prefeito(a)</p>
        </div>
      </div>`;
    const gestores = parseGestoresAtual(cheerio.load(html));
    assert.equal(gestores.length, 2);
    assert.equal(gestores[0].nome, 'Francisco Jardel Sousa Pinho');
    assert.equal(gestores[0].cargo, 'Prefeito(a)');
    assert.equal(gestores[1].cargo, 'Vice-Prefeito(a)');
  });
});

describe('licitacao-html', () => {
  it('remove linhas da comissão de licitação', () => {
    const filtered = filterValidLicitacoes([
      {
        numero: 'AGENTE DE CONTRATAÇÃO-LICITAÇÃO',
        modalidade: 'Antonio Nilo',
        objeto: 'Pregoeiro',
        situacao: '10/03/2025',
      },
      {
        numero: '12.002/2026-PE',
        modalidade: 'PREGÃO',
        objeto: 'Transporte escolar',
        situacao: 'Em andamento',
        dataAbertura: '26/06/2026',
      },
    ]);
    assert.equal(filtered.length, 1);
    assert.equal(filtered[0].numero, '12.002/2026-PE');
  });
});
