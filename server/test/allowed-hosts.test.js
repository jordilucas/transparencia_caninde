'use strict';

const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const {
  isAllowedOutboundUrl,
  assertAllowedOutboundUrl,
} = require('../lib/allowed-hosts');

describe('allowed-hosts', () => {
  it('permite portais oficiais HTTPS', () => {
    assert.equal(isAllowedOutboundUrl('https://www.caninde.ce.gov.br/contratos.php?id=1'), true);
    assert.equal(isAllowedOutboundUrl('https://cmcaninde.ce.gov.br/wp-json/wp/v2/vereadores'), true);
    assert.equal(isAllowedOutboundUrl('https://www.governotransparente.com.br/id/11979490'), true);
  });

  it('rejeita HTTP, hosts desconhecidos e URLs inválidas', () => {
    assert.equal(isAllowedOutboundUrl('http://www.caninde.ce.gov.br/x'), false);
    assert.equal(isAllowedOutboundUrl('https://evil.example.com/'), false);
    assert.equal(isAllowedOutboundUrl(''), false);
    assert.throws(() => assertAllowedOutboundUrl('https://evil.example.com/'));
  });
});
