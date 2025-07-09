package net.originmobi.pdv.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import net.originmobi.pdv.model.*;
import net.originmobi.pdv.repository.EmpresaParametrosRepository;
import net.originmobi.pdv.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
public class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private EmpresaParametrosRepository empresaParametrosRepository;

    @Mock
    private RegimeTributarioService regimeTributarioService;

    @Mock
    private CidadeService cidadeService;

    @Mock
    private EnderecoService enderecoService;

    @Mock
    private TransferenciaService transferenciaService;

    @InjectMocks
    private EmpresaService empresaService;

    @BeforeEach
    public void setup() {

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCadastroEmpresa() {
        Empresa empresa = new Empresa();
        empresa.setNome("Test Empresa");

        when(transferenciaService.cadastrar(anyDouble(), anyLong(), anyLong()))
                .thenReturn("Transferência realizada com sucesso");

        empresaService.cadastro(empresa);


        verify(empresaRepository, times(1)).save(empresa);
    }

    @Test
    public void testVerificaEmpresaCadastrada() {
        Empresa empresa = new Empresa();
        empresa.setNome("Empresa Existente");


        when(empresaRepository.buscaEmpresaCadastrada()).thenReturn(Optional.of(empresa));


        Optional<Empresa> result = empresaService.verificaEmpresaCadastrada();


        assertTrue(result.isPresent());
        assertEquals("Empresa Existente", result.get().getNome());
    }

    @Test
    public void testMerger_UpdateEmpresa() {
        Long codigo = 1L;
        String nome = "Nova Empresa";
        String nomeFantasia = "Nova Fantasia";
        String cnpj = "00.000.000/0001-00";
        String ie = "123456789";
        int serie = 1;
        int ambiente = 1;
        Long codRegime = 1L;
        Long codEndereco = 1L;
        Long codCidade = 1L;
        String rua = "Rua Exemplo";
        String bairro = "Bairro Exemplo";
        String numero = "123";
        String cep = "00000-000";
        String referencia = "Perto da praça";
        Double aliqCalcCredito = 0.18;

        RegimeTributario regime = new RegimeTributario();
        Cidade cidade = new Cidade();


        doNothing().when(empresaRepository).update(anyLong(), anyString(), anyString(), anyString(), anyString(), anyLong()); // Método void
        doNothing().when(empresaParametrosRepository).update(anyInt(), anyInt(), anyDouble()); // Método void
        doNothing().when(enderecoService).update(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()); // Método void
        when(regimeTributarioService.busca(anyLong())).thenReturn(Optional.of(regime));
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(cidade));


        String result = empresaService.merger(codigo, nome, nomeFantasia, cnpj, ie, serie, ambiente, codRegime, codEndereco, codCidade, rua, bairro, numero, cep, referencia, aliqCalcCredito);


        assertEquals("Empresa salva com sucesso", result);
    }

    @Test
    public void testMerger_CreateNewEmpresa() {
        Long codigo = null; // Simula uma criação de nova empresa
        String nome = "Nova Empresa";
        String nomeFantasia = "Fantasia Teste";
        String cnpj = "12345678000100";
        String ie = "123456789";
        int serie = 1;
        int ambiente = 1;
        Long codRegime = 1L;
        Long codEndereco = 1L;
        Long codCidade = 1L;
        String rua = "Rua Teste";
        String bairro = "Bairro Teste";
        String numero = "123";
        String cep = "12345-678";
        String referencia = "Em frente ao parque";
        Double aliqCalcCredito = 0.18;

        // Configuração correta dos mocks
        Empresa empresaMock = new Empresa();
        EmpresaParametro parametroMock = new EmpresaParametro();
        RegimeTributario regimeMock = new RegimeTributario();
        Cidade cidadeMock = new Cidade();

        when(empresaRepository.save(any(Empresa.class))).thenReturn(empresaMock);
        when(empresaParametrosRepository.save(any(EmpresaParametro.class))).thenReturn(parametroMock);
        when(enderecoService.cadastrar(any(Endereco.class))).thenReturn(new Endereco());
        when(regimeTributarioService.busca(anyLong())).thenReturn(Optional.of(regimeMock));
        when(cidadeService.busca(anyLong())).thenReturn(Optional.of(cidadeMock));

        String result = empresaService.merger(codigo, nome, nomeFantasia, cnpj, ie, serie, ambiente, codRegime,
                codEndereco, codCidade, rua, bairro, numero, cep, referencia, aliqCalcCredito);

        assertEquals("Empresa salva com sucesso", result);
    }
}