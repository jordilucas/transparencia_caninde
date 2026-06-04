'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const path = require('path');
const fs = require('fs');
const {
  mapContratos,
  mapLicitacoes,
  mapSecretarias,
  mapPublicacoes,
  isEmptyResponse,
} = require('../lib/scraper-prefeitura-dadosabertos');

const fixture = (name) =>
  JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures', name), 'utf8'));

describe('scraper-prefeitura-dadosabertos', () => {
  it('mapContratos formata valor e campos extras', () => {
    const rows = fixture('dadosabertos-contratos.json');
    const out = mapContratos(rows);
    assert.equal(out.length, 1);
    assert.equal(out[0].numero, '001/2025');
    assert.equal(out[0].secretaria, 'Saúde');
    assert.equal(out[0].cnpjCredor, '12.345.678/0001-90');
    assert.match(out[0].valor, /R\$/);
    assert.match(out[0].pdfUrl, /contrato\.pdf/);
  });

  it('mapLicitacoes usa NumeroPrecesso', () => {
    const out = mapLicitacoes([
      { Id: 2, NumeroPrecesso: '02/2025', Objeto: 'Obra', Modalidade: 'Concorrência', DataAbertura: '10/02/2025' },
    ]);
    assert.equal(out[0].numero, '02/2025');
    assert.equal(out[0].dataAbertura, '10/02/2025');
  });

  it('mapSecretarias preenche contato', () => {
    const out = mapSecretarias([
      { Id: 3, Secretaria: 'Educação', Gestor: 'Maria', Email: 'edu@caninde.ce.gov.br', Telefone1: '8834' },
    ]);
    assert.equal(out[0].nome, 'Educação');
    assert.equal(out[0].contato.email, 'edu@caninde.ce.gov.br');
  });

  it('mapPublicacoes resolve titulo e url', () => {
    const out = mapPublicacoes([
      { Id: 4, Descricao: 'Diário 01', TipoArquivo: 'PDF', Data: '01/03/2025', Url: '/publicacao.php?id=4' },
    ]);
    assert.equal(out[0].titulo, 'Diário 01');
    assert.match(out[0].url, /caninde\.ce\.gov\.br/);
  });
});

describe('isEmptyResponse', () => {
  it('detecta resposta sem registros', () => {
    assert.equal(isEmptyResponse('<SCRIPT>alert("Não há registros")'), true);
    assert.equal(isEmptyResponse('[]'), false);
  });
});
