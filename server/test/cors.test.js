'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  isAllowedOrigin,
  corsHeaders,
} = require('../lib/cors');

describe('cors', () => {
  const allowed = [
    'https://transparenciacaninde.com.br',
    'https://jordilucas.github.io',
  ];

  it('permite origem do site e localhost em desenvolvimento', () => {
    assert.equal(isAllowedOrigin('https://transparenciacaninde.com.br', allowed), true);
    assert.equal(isAllowedOrigin('http://localhost:8080', allowed), true);
    assert.equal(isAllowedOrigin('https://evil.example.com', allowed), false);
  });

  it('retorna Access-Control-Allow-Origin para origens permitidas', () => {
    const headers = corsHeaders('https://transparenciacaninde.com.br', allowed);
    assert.equal(headers['Access-Control-Allow-Origin'], 'https://transparenciacaninde.com.br');
    assert.deepEqual(corsHeaders('https://evil.example.com', allowed), {});
  });
});
