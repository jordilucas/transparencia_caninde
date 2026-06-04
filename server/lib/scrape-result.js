'use strict';

function joinErrors(parts) {
  const msgs = parts.filter(Boolean);
  return msgs.length > 0 ? msgs.join(' ') : null;
}

function buildPrefeituraPayload({
  contratos = [],
  licitacoes = [],
  diariosOficiais = [],
  secretarias = [],
  fonte,
  scrapeError = null,
}) {
  const missing = [];
  if (contratos.length === 0) missing.push('contratos');
  if (licitacoes.length === 0) missing.push('licitações');
  if (diariosOficiais.length === 0) missing.push('diário oficial');
  if (secretarias.length === 0) missing.push('secretarias');

  const partial =
    missing.length > 0 && missing.length < 4
      ? `Alguns dados não foram carregados: ${missing.join(', ')}.`
      : null;

  const allEmpty = missing.length === 4;
  const error = joinErrors([
    scrapeError,
    allEmpty ? 'Não foi possível obter dados da Prefeitura de Canindé. Tente atualizar mais tarde.' : null,
    partial,
  ]);

  return {
    municipio: 'Canindé',
    estado: 'CE',
    fonte: fonte || 'https://www.caninde.ce.gov.br/acessoainformacao.php',
    contratos,
    licitacoes,
    diariosOficiais,
    secretarias,
    resumo: {
      totalContratos: contratos.length,
      totalLicitacoes: licitacoes.length,
      exercicio: new Date().getFullYear(),
    },
    error,
  };
}

function buildCamaraPayload({
  parlamentares = [],
  sessoes = [],
  materias = [],
  mesaDiretora = [],
  fonte,
  scrapeError = null,
}) {
  const missing = [];
  if (parlamentares.length === 0) missing.push('vereadores');
  if (sessoes.length === 0) missing.push('sessões');
  if (materias.length === 0) missing.push('matérias');
  if (mesaDiretora.length === 0) missing.push('mesa diretora');

  const partial =
    missing.length > 0 && missing.length < 4
      ? `Alguns dados não foram carregados: ${missing.join(', ')}.`
      : null;

  const allEmpty = missing.length === 4;
  const error = joinErrors([
    scrapeError,
    allEmpty ? 'Não foi possível obter dados da Câmara Municipal de Canindé. Tente atualizar mais tarde.' : null,
    partial,
  ]);

  return {
    municipio: 'Canindé',
    estado: 'CE',
    fonte: fonte || 'https://www.cmcaninde.ce.gov.br/parlamentares/',
    parlamentares,
    sessoes,
    materias,
    mesaDiretora,
    resumoCamara: {
      totalParlamentares: parlamentares.length,
      totalSessoes2025: sessoes.length,
      totalMaterias: materias.length,
    },
    error,
  };
}

function basicPrefeituraShell(error) {
  return buildPrefeituraPayload({ scrapeError: error });
}

function basicCamaraShell(error) {
  return buildCamaraPayload({ scrapeError: error });
}

module.exports = {
  buildPrefeituraPayload,
  buildCamaraPayload,
  basicPrefeituraShell,
  basicCamaraShell,
};
