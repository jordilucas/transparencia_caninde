'use strict';

const BASE = 'https://www.cmcaninde.ce.gov.br';
const GT_CAMARA = '11979588';
const GT_BASE = 'https://www.governotransparente.com.br';

function buildLinksTransparenciaCamara() {
  const id = GT_CAMARA;
  return [
    { titulo: 'Portal Canindé Transparente', url: `${BASE}/caninde-transparente/`, categoria: 'camara' },
    { titulo: 'Receitas', url: `${GT_BASE}/transparencia/receitas/${id}?clean=false`, categoria: 'financeiro' },
    { titulo: 'Despesas', url: `${GT_BASE}/transparencia/despesas/opcoes/${id}?clean=false`, categoria: 'financeiro' },
    { titulo: 'Licitações e contratos', url: `${GT_BASE}/transparencia/${id}/consultarconvenio?clean=false`, categoria: 'compras' },
    { titulo: 'Pessoal', url: `${GT_BASE}/acessoinfo/${id}/despesasportipo?tipo=5&clean=false`, categoria: 'pessoal' },
    { titulo: 'Leis e publicações', url: `${BASE}/caninde-transparente/`, categoria: 'legislativo' },
    { titulo: 'LRF — Contas públicas', url: `${BASE}/caninde-transparente/lei-de-responsabilidade-fiscal-e-contas-publicas`, categoria: 'fiscal' },
    { titulo: 'e-SIC', url: `${BASE}/caninde-transparente/`, categoria: 'cidadania' },
  ];
}

function buildLinksTransparenciaPrefeitura() {
  const id = '11979490';
  return [
    { titulo: 'Receitas — Governo Transparente', url: `${GT_BASE}/transparencia/receitas/${id}?clean=false`, categoria: 'financeiro' },
    { titulo: 'Despesas detalhadas', url: `${GT_BASE}/transparencia/despesas/opcoes/${id}?clean=false`, categoria: 'financeiro' },
    { titulo: 'Convênios', url: `${GT_BASE}/transparencia/${id}/consultarconvenio?clean=false`, categoria: 'compras' },
    { titulo: 'Obras', url: `${GT_BASE}/transparencia/obras/${id}?clean=false`, categoria: 'obras' },
    { titulo: 'Emendas parlamentares', url: `${GT_BASE}/acessoinfo/${id}/consultaremendas?clean=false`, categoria: 'emendas' },
    { titulo: 'Dados abertos (Prefeitura)', url: 'https://www.caninde.ce.gov.br/dadosabertos.php', categoria: 'dadosabertos' },
  ];
}

module.exports = {
  buildLinksTransparenciaCamara,
  buildLinksTransparenciaPrefeitura,
};
