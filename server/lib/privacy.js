'use strict';

function emptyContato() {
  return { email: '', telefone: '', whatsapp: '', endereco: '', horarioFuncionamento: '' };
}

/** Remove dados de contato pessoal de vereadores (não exibidos no app). */
function stripParlamentarContato(parlamentar) {
  if (!parlamentar) return parlamentar;
  return { ...parlamentar, contato: emptyContato() };
}

function stripParlamentaresContato(parlamentares) {
  return (parlamentares || []).map(stripParlamentarContato);
}

module.exports = {
  emptyContato,
  stripParlamentarContato,
  stripParlamentaresContato,
};
