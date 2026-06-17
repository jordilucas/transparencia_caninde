'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  parseContratoHtmlRow,
  splitSecretariaObjeto,
  splitDataValor,
  splitEmpresaCnpj,
} = require('../lib/contrato-html');

describe('contrato-html', () => {
  it('separa secretaria e descrição coladas', () => {
    const { secretaria, objeto } = splitSecretariaObjeto(
      'Secretaria de Agricultura e Recursos HídricosCONTRATAÇÃO DE EMPRESA PARA REFORMA',
    );
    assert.equal(secretaria, 'Secretaria de Agricultura e Recursos Hídricos');
    assert.match(objeto, /CONTRATAÇÃO/);
  });

  it('separa data e valor colados', () => {
    const { data, valor } = splitDataValor('12/06/202665.000,00');
    assert.equal(data, '12/06/2026');
    assert.equal(valor, 'R$ 65.000,00');
  });

  it('separa empresa e CNPJ', () => {
    const { empresa, cnpjCredor } = splitEmpresaCnpj(
      'J SOLUÇÃO E PRESTAÇÃO DE SERVIÇOS LTDA 27.782.897/0001-09',
    );
    assert.equal(empresa, 'J SOLUÇÃO E PRESTAÇÃO DE SERVIÇOS LTDA');
    assert.equal(cnpjCredor, '27.782.897/0001-09');
  });

  it('parseContratoHtmlRow mapeia linha da tabela', () => {
    const row = parseContratoHtmlRow([
      '202606120002    CONTRATO ORIGINAL',
      'RDO ROBENYLSON FURTADO NOGUEIRA 49.627.786/0001-60',
      'Secretaria de Segurança Pública e Trânsito NOVO CONTRATAÇÃO DE EMPRESA',
      '12/06/202665.000,00',
      '12/06/2026  12/06/2027VIGENTE',
      'CADASTRADO 12/06/2026',
      '',
    ], 'contratos.php?id=1073');

    assert.equal(row.numero, '202606120002');
    assert.equal(row.secretaria, 'Secretaria de Segurança Pública e Trânsito');
    assert.match(row.objeto, /CONTRATAÇÃO/);
    assert.equal(row.valor, 'R$ 65.000,00');
    assert.equal(row.empresa, 'RDO ROBENYLSON FURTADO NOGUEIRA');
    assert.equal(row.cnpjCredor, '49.627.786/0001-60');
    assert.match(row.data, /Vigente/);
  });
});
