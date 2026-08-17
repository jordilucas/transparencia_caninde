'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { stripParlamentarContato, stripParlamentaresContato } = require('../lib/privacy');

describe('privacy', () => {
  it('stripParlamentarContato remove e-mail, telefone e whatsapp', () => {
    const stripped = stripParlamentarContato({
      nome: 'Maria',
      contato: {
        email: 'maria@example.com',
        telefone: '85999999999',
        whatsapp: '5585999999999',
        endereco: 'Rua A',
        horarioFuncionamento: '08h',
      },
    });
    assert.equal(stripped.nome, 'Maria');
    assert.deepEqual(stripped.contato, {
      email: '',
      telefone: '',
      whatsapp: '',
      endereco: '',
      horarioFuncionamento: '',
    });
  });

  it('stripParlamentaresContato limpa lista', () => {
    const list = stripParlamentaresContato([
      { nome: 'A', contato: { email: 'a@x.com' } },
      { nome: 'B', contato: { telefone: '123' } },
    ]);
    assert.equal(list.length, 2);
    assert.equal(list[0].contato.email, '');
    assert.equal(list[1].contato.telefone, '');
  });
});
