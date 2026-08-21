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
  const competencias = portalFolha?.competencias || [];
  const portalSetores = portalFolha?.porSetor || [];
  const gtSetores = gtFolhaSetores || [];
  const sstSetores = sstFolha?.disponivel ? mapSstSecretariaToSetor(sstFolha.porSecretaria) : [];
  const anoCompetencias = extractCompetenciaAno(competencias);
  const gtTotal = sumSetores(gtSetores);
  const portalTotal = sumSetores(portalSetores);
  const sstTotal = sumSetores(sstSetores);
  const useSst = sstSetores.length > 0;
  const useGt = !useSst && gtSetores.length > 0 && gtTotal > 0 && (gtTotal >= portalTotal || portalSetores.length === 0);
  const porSetor = useSst ? sstSetores : (useGt ? gtSetores : portalSetores);
  const fontePorSetor = useSst ? 'sst_quadro_pessoal' : (useGt ? 'governo_transparente' : 'portal_municipal');
  const totalNumerico = sumSetores(porSetor);

  const avisoParts = [];
  if (sstFolha?.competencia) {
    avisoParts.push(`Quadro de pessoal S&S (competência ${sstFolha.competencia}).`);
  } else if (anoCompetencias && String(exercicio) !== anoCompetencias) {
    avisoParts.push(
      `Gráfico mensal do portal municipal publicado apenas para ${anoCompetencias}.`,
    );
  }
  if (useSst) {
    avisoParts.push('Totais por secretaria via quadro de pessoal oficial (S&S Informática).');
  } else if (useGt) {
    avisoParts.push(
      `Totais por secretaria via Governo Transparente (exercício ${exercicio}).`,
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
    exercicio,
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
      || `https://www.caninde.ce.gov.br/lcpagamentos.php?ANO=${exercicio}`,
    fonteSstUrl: sstFolha?.fonteUrl || 'https://www.sstransparenciamunicipal.net/transparencia/transparenciaisapi.dll/$/?entcod=117',
    gtConsultaUrl: gtUrls.gtFolhaConsultaUrl || '',
  };
}

module.exports = {
  extractCompetenciaAno,
  mapSstSecretariaToSetor,
  mergeFolhaPagamento,
};
