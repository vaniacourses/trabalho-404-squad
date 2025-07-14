package net.originmobi.pdv.service;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.singleton.Aplicacao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CaixaService
 * Valida operações de abertura, fechamento e gestão de caixa
 */
class CaixaServiceTest {

    // Constantes para testes
    private static final Double VALOR_ABERTURA_INVALIDO = -100.0;
    private static final Double VALOR_ABERTURA_VALIDO = 100.0;
    private static final Double VALOR_CAIXA_1 = 200.0;
    private static final Double VALOR_CAIXA_2 = 150.0;
    private static final Long CAIXA_ID = 1L;
    private static final String SENHA_INCORRETA = "123";
    private static final String SENHA_CORRETA = "SenhaCorreta";
    private static final String SENHA_CORRETA_HASH = "senhaCorretaHash";
    private static final String DESCRICAO_CAIXA_DIARIO = "Caixa diário";
    private static final String DESCRICAO_CAIXA_1 = "Caixa 1";
    private static final String DESCRICAO_CAIXA_2 = "Caixa 2";
    private static final String USUARIO_TESTE = "teste";
    private static final String SENHA_HASH = "hashedPassword";
    
    // Mensagens de erro e sucesso
    private static final String MSG_CAIXA_ABERTO_ANTERIOR = "Existe caixa de dias anteriores em aberto, favor verifique";
    private static final String MSG_VALOR_INVALIDO = "Valor Informado é inválido";
    private static final String MSG_SENHA_INCORRETA = "Senha incorreta";
    private static final String MSG_CAIXA_FECHADO_SUCESSO = "Caixa fechado com sucesso";
    private static final String MSG_CAIXA_JA_FECHADO = "Caixa já esta fechado";

    @InjectMocks
    private CaixaService caixaService;

    @Mock
    private CaixaRepository caixaRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private CaixaLancamentoService lancamentoService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private Aplicacao aplicacaoMock;

    private Usuario usuarioLogado;

    // Variável para controlar o mock estático
    private MockedStatic<Aplicacao> aplicacaoMockedStatic;

    @BeforeEach
    void setUp() {
        // Inicializa todos os campos anotados com @Mock e @InjectMocks nesta classe.
        // openMocks() é o sucessor moderno do deprecated initMocks().
        MockitoAnnotations.openMocks(this);

        usuarioLogado = new Usuario();
        usuarioLogado.setUser(USUARIO_TESTE);
        usuarioLogado.setSenha(SENHA_HASH);

        // O mock estático precisa ser criado após a inicialização dos mocks
        aplicacaoMockedStatic = mockStatic(Aplicacao.class);
        when(Aplicacao.getInstancia()).thenReturn(aplicacaoMock);
        when(aplicacaoMock.getUsuarioAtual()).thenReturn(USUARIO_TESTE);
        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuarioLogado);
    }

    @AfterEach
    void tearDown() {
        if (aplicacaoMockedStatic != null) {
            aplicacaoMockedStatic.close();
        }
    }
    @Test
    @DisplayName("Teste do Caixa já aberto")
    void deveValidarCaixaJaAberto() {
        // Criação de um novo caixa
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);

        // Mock para simular um caixa em aberto
        when(caixaRepository.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        // Verifica se a exceção correta é lançada
        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.cadastro(caixa));
        assertEquals(MSG_CAIXA_ABERTO_ANTERIOR, exception.getMessage());
    }

    @Test
    @DisplayName("Teste de Valor Inválido")
    void deveValidarAberturaComValorInvalido() {
        // caixa com valor de abertura negativo
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);
        caixa.setValor_abertura(VALOR_ABERTURA_INVALIDO);

        // Verifica quando o valor de abertura é inválido
        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.cadastro(caixa));
        assertEquals(MSG_VALOR_INVALIDO, exception.getMessage());
    }

    @Test
    @DisplayName("Teste de valor válido")
    void deveAbrirCaixaComValorValido(){
        // caixa com valor de abertura válido
        Caixa caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);
        caixa.setValor_abertura(VALOR_ABERTURA_VALIDO);
        caixa.setDescricao("");

        Usuario usuario = new Usuario();
        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);
        when(caixaRepository.save(any(Caixa.class))).thenReturn(caixa);

        Long codigoCaixa = caixaService.cadastro(caixa);

        assertEquals(caixa.getCodigo(), codigoCaixa);
        assertEquals(DESCRICAO_CAIXA_DIARIO, caixa.getDescricao());
        assertEquals(usuario, caixa.getUsuario());
    }

    @Test
    @DisplayName("Teste de Fechar o caixa")
    void deveFecharCaixa(){
        Usuario usuario = new Usuario();
        usuario.setSenha(SENHA_CORRETA);

        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);

        String resultado = caixaService.fechaCaixa(CAIXA_ID, SENHA_INCORRETA);
        assertEquals(MSG_SENHA_INCORRETA, resultado);

        resultado = caixaService.fechaCaixa(CAIXA_ID, SENHA_CORRETA);
        assertEquals(MSG_CAIXA_FECHADO_SUCESSO, resultado);
    }
    @Test
    @DisplayName("Teste de Fechar Caixa Já Fechado")
    void deveValidarFechamentoCaixaJaFechado() {
        Usuario usuario = new Usuario();
        usuario.setSenha(SENHA_CORRETA_HASH);
        when(usuarioService.buscaUsuario(anyString())).thenReturn(usuario);

        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        when(passwordEncoder.matches(SENHA_CORRETA, usuario.getSenha())).thenReturn(true);

        Caixa caixa = new Caixa();
        java.sql.Timestamp dataHora = new java.sql.Timestamp(System.currentTimeMillis());
        caixa.setData_fechamento(dataHora);
        when(caixaRepository.findById(CAIXA_ID)).thenReturn(Optional.of(caixa));

        Exception exception = assertThrows(RuntimeException.class, () -> caixaService.fechaCaixa(CAIXA_ID, SENHA_CORRETA));
        assertEquals(MSG_CAIXA_JA_FECHADO, exception.getMessage());
    }

    @Test
    @DisplayName("Teste de Caixa Aberto")
    void deveVerificarCaixaAberto() {
        when(caixaRepository.caixaAberto()).thenReturn(Optional.of(new Caixa()));

        boolean resultado = caixaService.caixaIsAberto();

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Teste do método listaTodos()")
    void deveListarTodosCaixas() {
        // Simulando os valores a serem retornados pelo repositório
        List<Caixa> caixasEsperados = Collections.unmodifiableList(Arrays.asList(
                new Caixa(DESCRICAO_CAIXA_1, CaixaTipo.CAIXA, VALOR_ABERTURA_VALIDO, VALOR_CAIXA_1, null, null, null, null),
                new Caixa(DESCRICAO_CAIXA_2, CaixaTipo.COFRE, VALOR_ABERTURA_VALIDO, VALOR_CAIXA_2, null, null, null, null)
        ));

        when(caixaRepository.findByCodigoOrdenado()).thenReturn(caixasEsperados);

        // Chamada do método a ser testado
        List<Caixa> resultado = caixaService.listaTodos();

        // Verificando se a lista retornada é igual a esperada
        assertEquals(caixasEsperados, resultado);
        assertEquals(2, resultado.size());
        assertEquals(DESCRICAO_CAIXA_1, resultado.get(0).getDescricao());
        assertEquals(DESCRICAO_CAIXA_2, resultado.get(1).getDescricao());

        // Verificando se a interação com o mock ocorreu como esperado
        verify(caixaRepository, times(1)).findByCodigoOrdenado();
    }
}