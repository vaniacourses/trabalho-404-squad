package net.originmobi.pdv.service;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    @Mock
    private Aplicacao aplicacaoMock;

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
        assertEquals(exception.getMessage(),"Valor Informado é inválido");
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
    @Test
    @DisplayName("Teste de Fechar o caixa")
    void FecharCaixa(){
        Long caixa = 1L;
        String senhaErro = "123";


        Usuario usuario = new Usuario();
        usuario.setSenha("SenhaCorreta");

        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);

        String resultado = caixaService.fechaCaixa(caixa, senhaErro);

        assertEquals("Senha incorreta", resultado);

        resultado = caixaService.fechaCaixa(caixa, "SenhaCorreta");

        assertEquals("Caixa fechado com sucesso", resultado);
    }
    @Test
    @DisplayName("Teste de Fehcar Caixa Já Fechado")
    void TestFechaCaixaJaFechado() {
        Long caixaId = 1L;
        String senhaCorreta = "senhaCorreta";

        Usuario usuario = new Usuario();
        usuario.setSenha("senhaCorretaHash");
        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);

        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.matches(senhaCorreta, usuario.getSenha())).thenReturn(true);

        Caixa caixa = new Caixa();
        java.sql.Timestamp dataHora = new java.sql.Timestamp(System.currentTimeMillis());
        caixa.setData_fechamento(dataHora);
        when(caixaRepository.findById(caixaId)).thenReturn(Optional.of(caixa));

        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.fechaCaixa(caixaId, senhaCorreta));
        assertEquals("Caixa já esta fechado", exception.getMessage());
    }
    @Test
    void TesteCaixaAberto() {
        when(caixaRepository.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        boolean resultado = caixaService.caixaIsAberto();

        assertTrue(resultado);
    }
    @Test
    @DisplayName("Teste do metodo listaTodos()")
    void testListaTodosCaixas() {
        // Simulando os valores a serem retornados pelo repositório
        List<Caixa> caixasEsperados = Collections.unmodifiableList(Arrays.asList(
                new Caixa("Caixa 1", CaixaTipo.CAIXA, 100.0, 200.0, null, null, null, null) ,
                new Caixa("Caixa 2", CaixaTipo.COFRE, 50.0, 150.0, null, null, null, null)
        ));

        when(caixaRepository.findByCodigoOrdenado()).thenReturn(caixasEsperados);

        // Chamada do método a ser testado
        List<Caixa> resultado = caixaService.listaTodos();

        // Verificando se a lista retornada é igual a esperada
        assertEquals(caixasEsperados, resultado);
        assertEquals(2, resultado.size());
        assertEquals("Caixa 1", resultado.get(0).getDescricao());
        assertEquals("Caixa 2", resultado.get(1).getDescricao());

        // Verificando se a interação com o mock ocorreu como esperado
        verify(caixaRepository, times(1)).findByCodigoOrdenado();
    }
}