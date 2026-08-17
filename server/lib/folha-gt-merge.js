'use strict';

const { formatBRL } = require('./brl');

function extractCompetenciaAno(competencias) {
  for (const comp of competencias || []) {
    const match = String(comp.competencia || '').match(/(\d{4})/);
    if (match) return match[1];
  }
  return '';
}

function sumSetores(setores) {
  return (setores || []).reduce((sum, item) => sum + (item.totalPagoNumerico || 0), 0);
}

function mergeFolhaPagamento(portalFolha, gtFolhaSetores, exercicio, gtUrls = {}) {
  const competencias = portalFolha?.competencias || [];
  const portalSetores = portalFolha?.porSetor || [];
  const gtSetores = gtFolhaSetores || [];
  const anoCompetencias = extractCompetenciaAno(competencias);
  const gtTotal = sumSetores(gtSetores);
  const portalTotal = sumSetores(portalSetores);
  const useGt = gtSetores.length > 0 && gtTotal > 0 && (gtTotal >= portalTotal || portalSetores.length === 0);
  const porSetor = useGt ? gtSetores : portalSetores;
  const fontePorSetor = useGt ? 'governo_transparente' : 'portal_municipal';
  const totalNumerico = sumSetores(porSetor);

  const avisoParts = [];
  if (anoCompetencias && String(exercicio) !== anoCompetencias) {
    avisoParts.push(
      `Gráfico mensal do portal municipal publicado apenas para ${anoCompetencias}.`,
    );
  }
  if (useGt) {
    avisoParts.push(
      `Totais por secretaria via Governo Transparente (exercício ${exercicio}).`,
    );
  } else if (portalSetores.length > 0) {
    avisoParts.push(
      'Totais por secretaria via portal municipal (amostra parcial de pagamentos).',
    );
  }
  avisoParts.push('Sem nomes de servidores neste app (LGPD).');

  return {
    exercicio,
    competencias,
    porSetor,
    totalPagoSetores: totalNumerico > 0 ? formatBRL(totalNumerico) : '',
    avisoPrivacidade: portalFolha?.avisoPrivacidade
      || 'Exibimos apenas totais agregados já publicados na transparência. '
      + 'Nomes, matrículas e contracheques individuais não são replicados neste app (LGPD). '
      + 'Consulte o portal oficial para detalhes nominais.',
    avisoDados: avisoParts.join(' '),
    fontePorSetor,
    fonteUrl: portalFolha?.fonteUrl || 'https://www.caninde.ce.gov.br/folhadepagamento.php',
    fontePagamentosUrl: portalFolha?.fontePagamentosUrl
      || `https://www.caninde.ce.gov.br/lcpagamentos.php?ANO=${exercicio}`,
    gtConsultaUrl: gtUrls.gtFolhaConsultaUrl || '',
  };
}

module.exports = {
  extractCompetenciaAno,
  mergeFolhaPagamento,
};
