package net.originmobi.pdv.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.Transferencia;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.TransferenciaRepository;
import net.originmobi.pdv.singleton.Aplicacao;
import net.originmobi.pdv.utilitarios.DataAtual;

@Service
public class TransferenciaService {

	@Autowired
	private TransferenciaRepository transferencias;

	@Autowired
	private UsuarioService usuarios;

	@Autowired
	private CaixaService caixas;

	public String cadastrar(Double valor, Long origem, Long destino) {
		Aplicacao aplicacao = Aplicacao.getInstancia();
		DataAtual dataAtual = new DataAtual();

		Usuario usuario = usuarios.buscaUsuario(aplicacao.getUsuarioAtual());

		Optional<Caixa> caiOrigem = caixas.busca(origem);
		Optional<Caixa> caiDestino = caixas.busca(destino);

		if (caiOrigem.equals(caiDestino)) {
			throw new RuntimeException("Destino é inválido");
		}

		if (!caiOrigem.isPresent() || caiOrigem.map(Caixa::getData_fechamento).isPresent()) {
			throw new RuntimeException("Conta origem não está aberta, verifique");
		}

		if (!caiDestino.isPresent() || caiDestino.map(Caixa::getData_fechamento).isPresent()) {
			throw new RuntimeException("Conta destino não está aberta, verifique");
		}

		// Evitar chamar get() direto no Optional sem verificar
		double valorOrigem = caiOrigem
				.map(Caixa::getValor_total)
				.orElseThrow(() -> new RuntimeException("Conta origem não encontrada"));

		if (valorOrigem < valor) {
			throw new RuntimeException("Saldo insuficiente para realizar a transferência");
		}

		// Evitar vários map().get() no destino
		String descricaoDestino = caiDestino
				.map(Caixa::getDescricao)
				.orElse("N/A");

		String codigoDestino = caiDestino
				.map(c -> c.getCodigo() != null ? c.getCodigo().toString() : "N/A")
				.orElse("N/A");

		Transferencia transferencia = new Transferencia(
				valor,
				dataAtual.dataAtualTimeStamp(),
				caiOrigem.get(),
				caiDestino.get(),
				usuario,
				"Transferencia para o " + descricaoDestino + " " + codigoDestino
		);

		try {
			transferencias.save(transferencia);
		} catch (Exception e) {
			throw new RuntimeException("Erro ao realizar a transferência, chame o suporte");
		}

		return "Transferência realizada com sucesso";
	}
}