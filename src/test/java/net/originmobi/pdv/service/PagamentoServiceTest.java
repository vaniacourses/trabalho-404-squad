package net.originmobi.pdv.unit;

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
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.model.PagamentoTipo;
import net.originmobi.pdv.repository.PagamentoTipoRespository;
import net.originmobi.pdv.service.PagamentoTipoService;

/**
 * Teste estrutural/unitário para PagamentoTipoService
 * Testa a lógica interna dos métodos isoladamente usando mocks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Teste Estrutural - PagamentoTipoService")
class PagamentoTipoServiceUnitTest {

    @Mock
    private PagamentoTipoRespository pagamentoTipoRepository;

    @InjectMocks
    private PagamentoTipoService pagamentoTipoService;

    private PagamentoTipo pagamentoTipoMock;

    @BeforeEach
    void setUp() {
        pagamentoTipoMock = new PagamentoTipo();
        pagamentoTipoMock.setCodigo(1L);
        pagamentoTipoMock.setDescricao("Cartão de Crédito");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve cadastrar pagamento com forma única (à vista)")
    void deveCadastrarPagamentoFormaUnica() {
        // Arrange
        pagamentoTipoMock.setFormaPagamento("00");
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertNotNull(pagamentoTipoMock.getData_cadastro(), "Data de cadastro deve ser definida");
        assertEquals(Date.valueOf(LocalDate.now()), pagamentoTipoMock.getData_cadastro());
        assertEquals(1, pagamentoTipoMock.getQtd_parcelas(), "Deve ter 1 parcela para pagamento à vista");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve cadastrar pagamento com múltiplas formas (parcelado)")
    void deveCadastrarPagamentoMultiplasFormas() {
        // Arrange - Forma de pagamento parcelado em 3x
        pagamentoTipoMock.setFormaPagamento("01/02/03");
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertEquals(3, pagamentoTipoMock.getQtd_parcelas(), "Deve ter 3 parcelas para pagamento 01/02/03");
        assertNotNull(pagamentoTipoMock.getData_cadastro());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve processar forma de pagamento com espaços")
    void deveProcessarFormaPagamentoComEspacos() {
        // Arrange - Forma com espaços misturados
        pagamentoTipoMock.setFormaPagamento("00 01/02");
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        // O split deve processar corretamente: "00 01 02" = 3 elementos
        assertEquals(3, pagamentoTipoMock.getQtd_parcelas(), "Deve processar corretamente espaços e barras");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve listar todos os tipos de pagamento")
    void deveListarTodosTiposPagamento() {
        // Arrange
        List<PagamentoTipo> listaMock = Arrays.asList(
            pagamentoTipoMock,
            new PagamentoTipo("Dinheiro", null),
            new PagamentoTipo("PIX", null)
        );
        when(pagamentoTipoRepository.findAll()).thenReturn(listaMock);
        
        // Act
        List<PagamentoTipo> resultado = pagamentoTipoService.listar();
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findAll();
        assertNotNull(resultado, "Lista não deve ser nula");
        assertEquals(3, resultado.size(), "Deve retornar 3 tipos de pagamento");
        assertEquals("Cartão de Crédito", resultado.get(0).getDescricao());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve buscar tipo por código")
    void deveBuscarTipoPorCodigo() {
        // Arrange
        Long codigo = 1L;
        when(pagamentoTipoRepository.findByCodigoIn(codigo)).thenReturn(pagamentoTipoMock);
        
        // Act
        PagamentoTipo resultado = pagamentoTipoService.busca(codigo);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findByCodigoIn(codigo);
        assertNotNull(resultado, "Resultado não deve ser nulo");
        assertEquals(pagamentoTipoMock.getCodigo(), resultado.getCodigo());
        assertEquals("Cartão de Crédito", resultado.getDescricao());
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve retornar quantidade de parcelas como string")
    void deveRetornarQuantidadeParcelasComoString() {
        // Arrange
        Long codigo = 1L;
        String quantidadeEsperada = "3";
        when(pagamentoTipoRepository.quantidadeParcelar(codigo)).thenReturn(quantidadeEsperada);
        
        // Act
        String resultado = pagamentoTipoService.qtdParcelas(codigo);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).quantidadeParcelar(codigo);
        assertNotNull(resultado, "Resultado não deve ser nulo");
        assertEquals(quantidadeEsperada, resultado, "Deve retornar a quantidade como string");
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
        assertEquals(1, pagamentoTipoMock.getQtd_parcelas(), "Forma vazia deve resultar em 1 parcela");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve processar forma complexa com múltiplas barras")
    void deveProcessarFormaComplexaMultiplasBarras() {
        // Arrange - Forma complexa: à vista + 2x parcelado + 3x parcelado
        pagamentoTipoMock.setFormaPagamento("00/01/02/03/04");
        
        // Act
        pagamentoTipoService.cadastrar(pagamentoTipoMock);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).save(pagamentoTipoMock);
        assertEquals(5, pagamentoTipoMock.getQtd_parcelas(), "Deve processar corretamente 5 formas de pagamento");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve manter integridade ao buscar código inexistente")
    void deveManterIntegridadeAoBuscarCodigoInexistente() {
        // Arrange
        Long codigoInexistente = 999L;
        when(pagamentoTipoRepository.findByCodigoIn(codigoInexistente)).thenReturn(null);
        
        // Act
        PagamentoTipo resultado = pagamentoTipoService.busca(codigoInexistente);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).findByCodigoIn(codigoInexistente);
        assertNull(resultado, "Deve retornar null para código inexistente");
    }

    @Test
    @DisplayName("ESTRUTURAL: Deve manter integridade ao buscar quantidade de código inexistente")
    void deveManterIntegridadeAoBuscarQuantidadeCodigoInexistente() {
        // Arrange
        Long codigoInexistente = 999L;
        when(pagamentoTipoRepository.quantidadeParcelar(codigoInexistente)).thenReturn(null);
        
        // Act
        String resultado = pagamentoTipoService.qtdParcelas(codigoInexistente);
        
        // Assert
        verify(pagamentoTipoRepository, times(1)).quantidadeParcelar(codigoInexistente);
        assertNull(resultado, "Deve retornar null para código inexistente");
    }
}
