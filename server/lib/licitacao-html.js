'use strict';

const COMMISSION_MARKERS = [
  'AGENTE DE CONTRATA',
  'COMISSÃO',
  'COMISSAO',
  'WWW.',
  'SEDE DA PREFEITURA',
  'LARGO FRANCISCO',
  'M2A TECNOLOGIA',
  '- - -',
];

function isCommissionRow(item) {
  if (!item) return true;
  const blob = [
    item.numero,
    item.modalidade,
    item.objeto,
    item.situacao,
  ].join(' ').toUpperCase();
  return COMMISSION_MARKERS.some((m) => blob.includes(m));
}

function formatDateBR(value) {
  if (!value) return '';
  const s = String(value).trim();
  const iso = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) return `${iso[3]}/${iso[2]}/${iso[1]}`;
  return s;
}

function normalizeLicitacao(item) {
  if (!item || isCommissionRow(item)) return null;

  const numero = String(item.numero || item.id || '').trim();
  const objeto = String(item.objeto || '').trim();
  const modalidade = String(item.modalidade || '').trim();

  if (!numero && !objeto) return null;
  if (!objeto && modalidade && modalidade.split(' ').length >= 3 && !modalidade.includes('PREGÃO')) {
    // Linha HTML trocada: modalidade recebeu nome de pessoa.
    return null;
  }

  return {
    ...item,
    numero,
    objeto,
    modalidade,
    situacao: String(item.situacao || 'Em andamento').trim() || 'Em andamento',
    dataAbertura: formatDateBR(item.dataAbertura),
  };
}

function filterValidLicitacoes(list) {
  return (list || [])
    .map(normalizeLicitacao)
    .filter(Boolean);
}

module.exports = {
  isCommissionRow,
  normalizeLicitacao,
  filterValidLicitacoes,
  formatDateBR,
};
