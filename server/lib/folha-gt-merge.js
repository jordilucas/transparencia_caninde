'use strict';

const { formatBRL } = require('./brl');

const MESES = {
  JANEIRO: 1,
  FEVEREIRO: 2,
  MARCO: 3,
  MARÇO: 3,
  ABRIL: 4,
  MAIO: 5,
  JUNHO: 6,
  JULHO: 7,
  AGOSTO: 8,
  SETEMBRO: 9,
  OUTUBRO: 10,
  NOVEMBRO: 11,
  DEZEMBRO: 12,
};

function parseCompetenciaSortKey(text) {
  const s = String(text || '').trim().toUpperCase();
  if (!s) return 0;

  let match = s.match(/^(\d{2})\/(\d{4})$/);
  if (match) {
    return parseInt(match[2], 10) * 100 + parseInt(match[1], 10);
  }

  match = s.match(/^([A-ZÇÁÉÍÓÚÃ]+)\/(\d{4})$/);
  if (match) {
    const month = MESES[match[1]] || 0;
    return parseInt(match[2], 10) * 100 + month;
  }

  match = s.match(/(\d{4})/);
  if (match) return parseInt(match[1], 10) * 100;
  return 0;
}

function parseCompetenciaYear(text) {
  const key = parseCompetenciaSortKey(text);
  return key ? Math.floor(key / 100) : 0;
}

function sortCompetenciasDesc(competencias) {
  return [...(competencias || [])].sort(
    (a, b) => parseCompetenciaSortKey(b.competencia) - parseCompetenciaSortKey(a.competencia),
  );
}

function filterCompetenciasForReferencia(competencias, referenciaExercicio) {
  const sorted = sortCompetenciasDesc(competencias);
  if (!referenciaExercicio) return sorted;
  const sameYear = sorted.filter(
    (item) => parseCompetenciaYear(item.competencia) === referenciaExercicio,
  );
  return sameYear.length > 0 ? sameYear : sorted;
}

function extractCompetenciaAno(competencias) {
  const sorted = sortCompetenciasDesc(competencias);
  const year = parseCompetenciaYear(sorted[0]?.competencia);
  return year ? String(year) : '';
}

function resolveReferenciaFolha(sstFolha, portalFolha, requestedExercicio) {
  const now = new Date();
  const nowKey = now.getFullYear() * 100 + (now.getMonth() + 1);
  const sstKey = sstFolha?.disponivel ? parseCompetenciaSortKey(sstFolha.competencia) : 0;
  const sortedPortal = sortCompetenciasDesc(portalFolha?.competencias);
  const portalKey = sortedPortal.length ? parseCompetenciaSortKey(sortedPortal[0].competencia) : 0;
  const bestKey = Math.max(sstKey, portalKey, nowKey, (requestedExercicio || 0) * 100 + 12);
  const referenciaExercicio = Math.floor(bestKey / 100) || requestedExercicio || now.getFullYear();

  let referenciaCompetencia = '';
  if (sstKey >= portalKey && sstFolha?.competencia) {
    referenciaCompetencia = sstFolha.competencia;
  } else if (sortedPortal[0]?.competencia) {
    referenciaCompetencia = sortedPortal[0].competencia;
  }

  return {
    referenciaExercicio,
    referenciaCompetencia,
    competenciasSorted: sortedPortal,
    sstKey,
    portalKey,
    requestedExercicio: requestedExercicio || referenciaExercicio,
  };
}

function sumSetores(setores) {
  return (setores || []).reduce((sum, item) => sum + (item.totalPagoNumerico || 0), 0);
}

function mapSstSecretariaToSetor(rows) {
  return (rows || []).map((row) => ({
    secretaria: row.nome,
    codigoOrgao: '',
    totalPago: row.bruto || formatBRL(row.brutoNumerico),
    totalPagoNumerico: row.brutoNumerico || 0,
    quantidadePagamentos: row.servidores || 0,
  }));
}

function mergeFolhaPagamento(portalFolha, gtFolhaSetores, exercicio, gtUrls = {}, sstFolha = null) {
  const referencia = resolveReferenciaFolha(sstFolha, portalFolha, exercicio);
  const {
    referenciaExercicio,
    referenciaCompetencia,
    competenciasSorted,
    sstKey,
    portalKey,
    requestedExercicio,
  } = referencia;

  const portalSetores = portalFolha?.porSetor || [];
  const gtSetores = gtFolhaSetores || [];
  const sstSetores = sstFolha?.disponivel ? mapSstSecretariaToSetor(sstFolha.porSecretaria) : [];
  const gtTotal = sumSetores(gtSetores);
  const portalTotal = sumSetores(portalSetores);
  const useSst = sstSetores.length > 0;
  const useGt = !useSst && gtSetores.length > 0 && gtTotal > 0 && (gtTotal >= portalTotal || portalSetores.length === 0);
  const porSetor = useSst ? sstSetores : (useGt ? gtSetores : portalSetores);
  const fontePorSetor = useSst ? 'sst_quadro_pessoal' : (useGt ? 'governo_transparente' : 'portal_municipal');
  const totalNumerico = sumSetores(porSetor);
  const competencias = filterCompetenciasForReferencia(competenciasSorted, referenciaExercicio);
  const anoCompetencias = extractCompetenciaAno(competenciasSorted);

  const avisoParts = [];
  if (referenciaCompetencia) {
    avisoParts.push(`Referência mais recente: competência ${referenciaCompetencia}.`);
  }
  if (requestedExercicio && referenciaExercicio > requestedExercicio) {
    avisoParts.push(
      `Exercício ${requestedExercicio} no portal municipal; exibimos a competência mais atual disponível (${referenciaCompetencia || referenciaExercicio}).`,
    );
  } else if (!useSst && anoCompetencias && String(referenciaExercicio) !== anoCompetencias) {
    avisoParts.push(
      `Gráfico mensal do portal municipal publicado apenas para ${anoCompetencias}.`,
    );
  } else if (!useSst && portalKey && sstKey === 0 && portalKey < referenciaExercicio * 100) {
    avisoParts.push('Portal municipal pode estar desatualizado em relação ao ano corrente.');
  }
  if (useSst) {
    avisoParts.push('Totais por secretaria via quadro de pessoal oficial (S&S Informática).');
  } else if (useGt) {
    avisoParts.push(
      `Totais por secretaria via Governo Transparente (exercício ${referenciaExercicio}).`,
    );
  } else if (portalSetores.length > 0) {
    avisoParts.push(
      'Totais por secretaria via portal municipal (amostra parcial de pagamentos).',
    );
  }
  if (sstFolha?.funcaoParcial) {
    avisoParts.push('Funções: amostra da 1ª página do quadro oficial.');
  }
  avisoParts.push('Sem nomes de servidores neste app (LGPD).');

  return {
    exercicio: referenciaExercicio,
    referenciaExercicio,
    referenciaCompetencia,
    competencias,
    porSetor,
    porNatureza: sstFolha?.porNatureza || [],
    porFuncao: sstFolha?.porFuncao || [],
    competenciaSst: sstFolha?.competencia || '',
    totalServidoresSst: sstFolha?.totalServidores || 0,
    totalPagoSetores: totalNumerico > 0 ? formatBRL(totalNumerico) : '',
    avisoPrivacidade: portalFolha?.avisoPrivacidade
      || 'Exibimos apenas totais agregados já publicados na transparência. '
      + 'Nomes, matrículas e contracheques individuais não são replicados neste app (LGPD). '
      + 'Consulte o portal oficial para detalhes nominais.',
    avisoDados: avisoParts.join(' '),
    fontePorSetor,
    fonteUrl: portalFolha?.fonteUrl || 'https://www.caninde.ce.gov.br/folhadepagamento.php',
    fontePagamentosUrl: portalFolha?.fontePagamentosUrl
      || `https://www.caninde.ce.gov.br/lcpagamentos.php?ANO=${referenciaExercicio}`,
    fonteSstUrl: sstFolha?.fonteUrl || 'https://www.sstransparenciamunicipal.net/transparencia/transparenciaisapi.dll/$/?entcod=117',
    gtConsultaUrl: gtUrls.gtFolhaConsultaUrl || '',
  };
}

module.exports = {
  MESES,
  parseCompetenciaSortKey,
  parseCompetenciaYear,
  sortCompetenciasDesc,
  filterCompetenciasForReferencia,
  resolveReferenciaFolha,
  extractCompetenciaAno,
  mapSstSecretariaToSetor,
  mergeFolhaPagamento,
};
