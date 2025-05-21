package net.originmobi.pdv.service;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

public class CaixaServiceTest {

    @InjectMocks
    private CaixaService caixaService;

    @Mock
    private CaixaRepository caixaRepository;

    @Mock
    private UsuarioService usuarioService;


    @BeforeEach
    void inicio() {
        MockitoAnnotations.initMocks(this);
    }
    
    //testando o metodo abrir caixa
    @Test
    @DisplayName("Teste do Caixa já aberto")
    void testAbrirCaixa() {
        // Criação de um novo caixa
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);

        // Mock para simular um caixa em aberto
        when(caixaRepository.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        // Verifica se a exceção correta é lançada
        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.cadastro(caixa));
        assertEquals("Existe caixa de dias anteriores em aberto, favor verifique", exception.getMessage());

    }
    @Test
    @DisplayName("Teste de Valor Invalido")
    void AberturaValorInvalido() {
        // caixa com valor de abertura negativo
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);
        caixa.setValor_abertura(-100.0);

        // Verifica quando o valor de abertura é inválido
        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.cadastro(caixa));
        assertEquals("Valor informado é inválido", exception.getMessage());
    }

    @Test
    @DisplayName("Teste de valor valido")
    void AberturaValorValido(){
        // caixa com valor de abertura válido
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);
        caixa.setValor_abertura(100.0);
        caixa.setDescricao("");

        Usuario usuario = new Usuario();
        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);

        when(caixaRepository.save(any(Caixa.class))).thenReturn(caixa);

        Long codigoCaixa = caixaService.cadastro(caixa);

        assertEquals(caixa.getCodigo(), codigoCaixa);
        assertEquals("Caixa diário", caixa.getDescricao());
        assertEquals(usuario, caixa.getUsuario());
    }
}