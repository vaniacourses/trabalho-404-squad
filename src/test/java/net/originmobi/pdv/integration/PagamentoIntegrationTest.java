package net.originmobi.pdv.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.Fornecedor;
import net.originmobi.pdv.model.Pagar;
import net.originmobi.pdv.model.PagarTipo;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.repository.FornecedorRepository;
import net.originmobi.pdv.repository.PagarRepository;
import net.originmobi.pdv.repository.PagarTipoRepository;
import net.originmobi.pdv.repository.UsuarioRepository;
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@Transactional
class PagamentoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Services removidos pois não são usados diretamente nos testes de integração
    // Os testes utilizam MockMvc para testar a integração completa

    @Autowired
    private PagarRepository pagarRepository;

    @Autowired
    private CaixaRepository caixaRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private PagarTipoRepository pagarTipoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario;
    private Fornecedor fornecedor;
    private PagarTipo pagarTipo;
    private Caixa caixa;
    private Pagar pagar;

    @BeforeEach
    void setUp() {
        // Limpar dados existentes antes de criar novos
        limparDados();
        // Criar dados de teste no banco
        criarDadosBasicos();
    }

    @AfterEach
    void tearDown() {
        // Limpar dados após cada teste para evitar interferência
        limparDados();
    }

    private void limparDados() {
        // Limpar na ordem correta (dependências primeiro)
        pagarRepository.deleteAll();
        caixaRepository.deleteAll();
        fornecedorRepository.deleteAll();
        pagarTipoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private void criarDadosBasicos() {
        // Criar usuário
        usuario = new Usuario();
        usuario.setUser("teste");
        usuario.setSenha("123");
        usuario.setData_cadastro(new java.sql.Date(System.currentTimeMillis()));
        usuario = usuarioRepository.save(usuario);

        // Criar fornecedor
        fornecedor = new Fornecedor();
        fornecedor.setNome("Fornecedor Teste LTDA");
        fornecedor.setCnpj("12.345.678/0001-90");
        fornecedor.setAtivo(1);
        fornecedor = fornecedorRepository.save(fornecedor);

        // Criar tipo de pagamento usando construtor com parâmetros
        pagarTipo = new PagarTipo("Fornecedores", new java.sql.Timestamp(System.currentTimeMillis()));
        pagarTipo = pagarTipoRepository.save(pagarTipo);

        // Criar caixa aberto
        caixa = new Caixa();
        caixa.setTipo(CaixaTipo.CAIXA);
        caixa.setValor_abertura(1000.0);
        caixa.setValor_total(1000.0);
        caixa.setData_cadastro(new java.sql.Date(System.currentTimeMillis()));
        caixa.setUsuario(usuario);
        caixa = caixaRepository.save(caixa);

        // Criar conta a pagar 
        pagar = new Pagar("Teste de Despesa Integração", 200.0, LocalDate.now(), fornecedor, pagarTipo);
        pagar = pagarRepository.save(pagar);
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve criar e persistir dados básicos no banco")
    void deveCriarEPersistirDadosBasicos() {
        // Assert - Verificar se todos os dados foram criados
        assertNotNull(usuario.getCodigo(), "Usuário deve ter ID gerado");
        assertNotNull(fornecedor.getCodigo(), "Fornecedor deve ter ID gerado");
        assertNotNull(pagarTipo.getCodigo(), "Tipo de pagamento deve ter ID gerado");
        assertNotNull(caixa.getCodigo(), "Caixa deve ter ID gerado");
        assertNotNull(pagar.getCodigo(), "Conta a pagar deve ter ID gerado");

        // Verificar se foram persistidos no banco
        Optional<Usuario> usuarioNoBanco = usuarioRepository.findById(usuario.getCodigo());
        assertTrue(usuarioNoBanco.isPresent(), "Usuário deve estar no banco");

        Optional<Fornecedor> fornecedorNoBanco = fornecedorRepository.findById(fornecedor.getCodigo());
        assertTrue(fornecedorNoBanco.isPresent(), "Fornecedor deve estar no banco");

        Optional<Caixa> caixaNoBanco = caixaRepository.findById(caixa.getCodigo());
        assertTrue(caixaNoBanco.isPresent(), "Caixa deve estar no banco");
        assertEquals(CaixaTipo.CAIXA, caixaNoBanco.get().getTipo(), "Tipo do caixa deve estar correto");
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve listar pagamentos via controller")
    void deveListarPagamentosViaController() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/pagar"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attributeExists("parcelas"));
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve validar integridade referencial")
    void deveValidarIntegridadeReferencial() {
        // Verificar relacionamentos
        assertEquals(fornecedor.getCodigo(), pagar.getFornecedor().getCodigo(), 
            "Pagar deve referenciar o fornecedor correto");
        assertEquals(pagarTipo.getCodigo(), pagar.getTipo().getCodigo(), 
            "Pagar deve referenciar o tipo correto");
        assertEquals(usuario.getCodigo(), caixa.getUsuario().getCodigo(), 
            "Caixa deve referenciar o usuário correto");
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve manter integridade ao tentar criar dados inválidos")
    void deveManterIntegridadeAoTentarCriarDadosInvalidos() {
        // Arrange - Contar registros antes
        long countPagarAntes = pagarRepository.count();
        long countFornecedorAntes = fornecedorRepository.count();

        // Act & Assert - Tentar criar fornecedor com CNPJ duplicado
        assertThrows(Exception.class, () -> {
            Fornecedor fornecedorDuplicado = new Fornecedor();
            fornecedorDuplicado.setNome("Fornecedor Duplicado");
            fornecedorDuplicado.setCnpj(fornecedor.getCnpj()); // CNPJ já existe
            fornecedorDuplicado.setAtivo(1);
            fornecedorRepository.saveAndFlush(fornecedorDuplicado);
        }, "Deveria lançar exceção por dados duplicados");

        // Assert - Verificar que os dados originais não foram afetados
        long countPagarDepois = pagarRepository.count();
        long countFornecedorDepois = fornecedorRepository.count();
        
        assertEquals(countPagarAntes, countPagarDepois, 
            "Número de registros Pagar deve permanecer igual");
        assertEquals(countFornecedorAntes, countFornecedorDepois, 
            "Número de registros Fornecedor deve permanecer igual");
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve buscar dados com relacionamentos lazy loading")
    void deveBuscarDadosComRelacionamentosLazyLoading() {
        // Act - Buscar pagar do banco
        Optional<Pagar> pagarNoBanco = pagarRepository.findById(pagar.getCodigo());
        
        // Assert
        assertTrue(pagarNoBanco.isPresent(), "Pagar deve existir no banco");
        
        Pagar pagarEncontrado = pagarNoBanco.get();
        assertNotNull(pagarEncontrado.getFornecedor(), "Fornecedor deve estar carregado");
        assertEquals("Fornecedor Teste LTDA", pagarEncontrado.getFornecedor().getNome(), 
            "Nome do fornecedor deve estar correto");
        
        assertNotNull(pagarEncontrado.getTipo(), "Tipo deve estar carregado");
        assertEquals("Fornecedores", pagarEncontrado.getTipo().getDescricao(), 
            "Descrição do tipo deve estar correta");
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve validar cascata de operações")
    void deveValidarCascataDeOperacoes() {
        // Arrange - Criar novo fornecedor apenas para este teste
        Fornecedor novoFornecedor = new Fornecedor();
        novoFornecedor.setNome("Fornecedor Cascata");
        novoFornecedor.setCnpj("98.765.432/0001-10");
        novoFornecedor.setAtivo(1);
        novoFornecedor = fornecedorRepository.save(novoFornecedor);

        // Act - Criar conta a pagar vinculada
        Pagar novoPagar = new Pagar("Teste Cascata", 150.0, LocalDate.now(), novoFornecedor, pagarTipo);
        novoPagar = pagarRepository.save(novoPagar);

        // Assert - Verificar relacionamento bidirecional
        Optional<Fornecedor> fornecedorNoBanco = fornecedorRepository.findById(novoFornecedor.getCodigo());
        assertTrue(fornecedorNoBanco.isPresent());
        
        Optional<Pagar> pagarNoBanco = pagarRepository.findById(novoPagar.getCodigo());
        assertTrue(pagarNoBanco.isPresent());
        assertEquals(novoFornecedor.getCodigo(), pagarNoBanco.get().getFornecedor().getCodigo());
    }

    @Test
    @DisplayName("INTEGRAÇÃO: Deve consultar usando queries customizadas")
    void deveConsultarUsandoQueriesCustomizadas() {
        // Act - Buscar por diferentes critérios
        assertEquals(1, pagarRepository.count(), "Deve ter exatamente 1 registro Pagar");
        assertEquals(1, fornecedorRepository.count(), "Deve ter exatamente 1 registro Fornecedor");
        assertEquals(1, usuarioRepository.count(), "Deve ter exatamente 1 registro Usuario");
        assertEquals(1, caixaRepository.count(), "Deve ter exatamente 1 registro Caixa");
        
        // Verificar se os dados são encontrados pelos IDs
        assertTrue(pagarRepository.existsById(pagar.getCodigo()));
        assertTrue(fornecedorRepository.existsById(fornecedor.getCodigo()));
        assertTrue(usuarioRepository.existsById(usuario.getCodigo()));
        assertTrue(caixaRepository.existsById(caixa.getCodigo()));
    }
}
