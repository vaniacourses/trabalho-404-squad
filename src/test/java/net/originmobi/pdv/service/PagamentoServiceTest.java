package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.repository.PagamentoTipoRespository;
import net.originmobi.pdv.service.PagamentoTipoService;

/**
 * Teste estrutural/unitário para PagamentoTipoService
 * Testa a lógica interna dos métodos isoladamente usando mocks
 */
//@ExtendWith(MockitoExtension.class)
@DisplayName("Teste Estrutural - PagamentoTipoService")
class PagamentoServiceTest {

    // Constantes para testes
    private static final Long CODIGO_PADRAO = 1L;
    private static final Long CODIGO_DINHEIRO = 2L;
    private static final Long CODIGO_PIX = 3L;
    private static final Long CODIGO_INEXISTENTE = 999L;
    private static final String DESCRICAO_CARTAO = "Cartão de Crédito";
    private static final String DESCRICAO_DINHEIRO = "Dinheiro";
    private static final String DESCRICAO_PIX = "PIX";
    private static final String FORMA_PAGAMENTO_VISTA = "00";
    private static final String FORMA_PAGAMENTO_PARCELADO = "01/02/03";
    private static final String FORMA_PAGAMENTO_ESPACOS = "00 01/02";
    private static final String FORMA_PAGAMENTO_COMPLEXA = "00/01/02/03/04";
    private static final String QUANTIDADE_ESPERADA = "3";
    private static final int PARCELAS_VISTA = 1;
    private static final int PARCELAS_TRES = 3;
    private static final int PARCELAS_CINCO = 5;
    private static final int TAMANHO_LISTA_ESPERADO = 3;

    @Mock
    private PagamentoTipoRespository pagamentoTipoRepository;

    @InjectMocks
    private PagamentoTipoService pagamentoTipoService;

    private PagamentoTipo pagamentoTipoMock;

    @BeforeEach
    void setUp() {
        pagamentoTipoMock = new PagamentoTipo();
        pagamentoTipoMock.setCodigo(CODIGO_PADRAO);
        pagamentoTipoMock.setDescricao(DESCRICAO_CARTAO);
        pagamentoTipoMock.setFormaPagamento(FORMA_PAGAMENTO_VISTA); // Inicializa com valor padrão
    }

    /**
     * Método auxiliar para criar instâncias de PagamentoTipo
     */
    private PagamentoTipo criarPagamentoTipo(Long codigo, String descricao) {
        PagamentoTipo tipo = new PagamentoTipo();
        tipo.setCodigo(codigo);
        tipo.setDescricao(descricao);
        return tipo;
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve cadastrar pagamento com forma única (à vista)")
    void deveCadastrarPagamentoFormaUnica() {
        // Arrange
        pagamentoTipoMock.setFormaPagamento(FORMA_PAGAMENTO_VISTA);
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertNotNull(pagamentoTipoMock.getData_cadastro(), "Data de cadastro deve ser definida");
        assertEquals(Date.valueOf(LocalDate.now()), pagamentoTipoMock.getData_cadastro());
        assertEquals(PARCELAS_VISTA, pagamentoTipoMock.getQtd_parcelas(), "Deve ter 1 parcela para pagamento à vista");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve cadastrar pagamento com múltiplas formas (parcelado)")
    void deveCadastrarPagamentoMultiplasFormas() {
        // Arrange - Forma de pagamento parcelado em 3x
        pagamentoTipoMock.setFormaPagamento(FORMA_PAGAMENTO_PARCELADO);
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertEquals(PARCELAS_TRES, pagamentoTipoMock.getQtd_parcelas(), "Deve ter 3 parcelas para pagamento 01/02/03");
        assertNotNull(pagamentoTipoMock.getData_cadastro());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve processar forma de pagamento com espaços")
    void deveProcessarFormaPagamentoComEspacos() {
        // Arrange - Forma com espaços misturados
        pagamentoTipoMock.setFormaPagamento(FORMA_PAGAMENTO_ESPACOS);
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        // O split deve processar corretamente: "00 01 02" = 3 elementos
        assertEquals(PARCELAS_TRES, pagamentoTipoMock.getQtd_parcelas(), "Deve processar corretamente espaços e barras");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve listar todos os tipos de pagamento")
    void deveListarTodosTiposPagamento() {
        // Arrange
        List<PagamentoTipo> listaMock = Arrays.asList(
            pagamentoTipoMock,
            criarPagamentoTipo(CODIGO_DINHEIRO, DESCRICAO_DINHEIRO),
            criarPagamentoTipo(CODIGO_PIX, DESCRICAO_PIX)
        );
        when(pagamentoTipoRepository.findAll()).thenReturn(listaMock);
        
        // Act
        List<PagamentoTipo> resultado = pagamentoTipoService.listar();
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findAll();
        assertNotNull(resultado, "Lista não deve ser nula");
        assertEquals(TAMANHO_LISTA_ESPERADO, resultado.size(), "Deve retornar 3 tipos de pagamento");
        assertEquals(DESCRICAO_CARTAO, resultado.get(0).getDescricao());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve buscar tipo por código")
    void deveBuscarTipoPorCodigo() {
        // Arrange
        when(pagamentoTipoRepository.findByCodigoIn(CODIGO_PADRAO)).thenReturn(pagamentoTipoMock);
        
        // Act
        PagamentoTipo resultado = pagamentoTipoService.busca(CODIGO_PADRAO);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findByCodigoIn(CODIGO_PADRAO);
        assertNotNull(resultado, "Resultado não deve ser nulo");
        assertEquals(pagamentoTipoMock.getCodigo(), resultado.getCodigo());
        assertEquals(DESCRICAO_CARTAO, resultado.getDescricao());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve retornar quantidade de parcelas como string")
    void deveRetornarQuantidadeParcelasComoString() {
        // Arrange
        when(pagamentoTipoRepository.quantidadeParcelar(CODIGO_PADRAO)).thenReturn(QUANTIDADE_ESPERADA);
        
        // Act
        String resultado = pagamentoTipoService.qtdParcelas(CODIGO_PADRAO);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).quantidadeParcelar(CODIGO_PADRAO);
        assertNotNull(resultado, "Resultado não deve ser nulo");
        assertEquals(QUANTIDADE_ESPERADA, resultado, "Deve retornar a quantidade como string");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve processar forma de pagamento vazia")
    void deveProcessarFormaPagamentoVazia() {
        // Arrange
        pagamentoTipoMock.setFormaPagamento("");
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertEquals(PARCELAS_VISTA, pagamentoTipoMock.getQtd_parcelas(), "Forma vazia deve resultar em 1 parcela");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve processar forma complexa com múltiplas barras")
    void deveProcessarFormaComplexaMultiplasBarras() {
        // Arrange - Forma complexa: à vista + 2x parcelado + 3x parcelado
        pagamentoTipoMock.setFormaPagamento(FORMA_PAGAMENTO_COMPLEXA);
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertEquals(PARCELAS_CINCO, pagamentoTipoMock.getQtd_parcelas(), "Deve processar corretamente 5 formas de pagamento");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve manter integridade ao buscar código inexistente")
    void deveManterIntegridadeAoBuscarCodigoInexistente() {
        // Arrange
        when(pagamentoTipoRepository.findByCodigoIn(CODIGO_INEXISTENTE)).thenReturn(null);
        
        // Act
        PagamentoTipo resultado = pagamentoTipoService.busca(CODIGO_INEXISTENTE);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findByCodigoIn(CODIGO_INEXISTENTE);
        assertNull(resultado, "Deve retornar null para código inexistente");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve manter integridade ao buscar quantidade de código inexistente")
    void deveManterIntegridadeAoBuscarQuantidadeCodigoInexistente() {
        // Arrange
        when(pagamentoTipoRepository.quantidadeParcelar(CODIGO_INEXISTENTE)).thenReturn(null);
        
        // Act
        String resultado = pagamentoTipoService.qtdParcelas(CODIGO_INEXISTENTE);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).quantidadeParcelar(CODIGO_INEXISTENTE);
        assertNull(resultado, "Deve retornar null para código inexistente");
    }
}
