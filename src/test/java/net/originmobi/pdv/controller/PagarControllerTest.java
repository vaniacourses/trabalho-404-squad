package net.originmobi.pdv.controller;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import net.originmobi.pdv.filter.PagarParcelaFilter;
import net.originmobi.pdv.model.Fornecedor;
import net.originmobi.pdv.model.Pagar;
import net.originmobi.pdv.model.PagarParcela;
import net.originmobi.pdv.model.PagarTipo;
import net.originmobi.pdv.service.CaixaService;
import net.originmobi.pdv.service.FornecedorService;
import net.originmobi.pdv.service.PagarParcelaService;
import net.originmobi.pdv.service.PagarService;
import net.originmobi.pdv.service.PagarTipoService;

@WebMvcTest(PagarController.class)
class PagarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagarService pagarService;

    @MockBean
    private PagarParcelaService pagarParcelaService;

    @MockBean
    private FornecedorService fornecedorService;

    @MockBean
    private PagarTipoService pagarTipoService;

    @MockBean
    private CaixaService caixaService;

    private List<PagarParcela> parcelas;
    private Page<PagarParcela> paginaParcelas;
    private PagarParcelaFilter filter;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Configuração inicial dos dados de teste
        parcelas = new ArrayList<>();
        
        // Criando dados de teste
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setCodigo(1L);
        fornecedor.setNome("Fornecedor Teste");

        PagarTipo tipo = new PagarTipo();
        tipo.setCodigo(1L);
        tipo.setDescricao("Despesa Teste");

        Pagar pagar = new Pagar();
        pagar.setCodigo(1L);
        pagar.setObservacao("Teste de Despesa");
        pagar.setValor_total(100.0);
        pagar.setFornecedor(fornecedor);
        pagar.setTipo(tipo);

        PagarParcela parcela1 = new PagarParcela();
        parcela1.setCodigo(1L);
        parcela1.setValor_total(50.0);
        parcela1.setValor_pago(0.0);
        parcela1.setData_vencimento(LocalDate.now());
        parcela1.setPagar(pagar);

        PagarParcela parcela2 = new PagarParcela();
        parcela2.setCodigo(2L);
        parcela2.setValor_pago(50.0);
        parcela2.setValor_pago(0.0);
        parcela2.setData_vencimento(LocalDate.now().plusMonths(1));
        parcela2.setPagar(pagar);

        parcelas.add(parcela1);
        parcelas.add(parcela2);

        pageable = PageRequest.of(0, 10);
        paginaParcelas = new PageImpl<>(parcelas, pageable, parcelas.size());
        
        filter = new PagarParcelaFilter();
    }

    @AfterEach
    void tearDown() {
        // Limpar todos os mocks após cada teste para garantir isolamento
        Mockito.reset(
            pagarService,
            pagarParcelaService,
            fornecedorService,
            pagarTipoService,
            caixaService
        );
    }

    @Test
    @DisplayName("Deve listar todas as parcelas pendentes quando não houver filtro")
    void deveListarTodasParcelasPendentes() throws Exception {
        // Arrange
        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaParcelas);

        // Act & Assert
        mockMvc.perform(get("/pagar"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", hasSize(2)))
            .andExpect(model().attribute("qtdpaginas", is(1)))
            .andExpect(model().attribute("pagAtual", is(0)));

        // Verify
        verify(pagarParcelaService, times(1)).lista(any(PagarParcelaFilter.class), any(Pageable.class));
        verifyNoMoreInteractions(fornecedorService);
        verifyNoMoreInteractions(pagarTipoService);
    }

    @Test
    @DisplayName("Deve filtrar parcelas por fornecedor")
    void deveListarParcelasPorFornecedor() throws Exception {
        filter.setNome("Fornecedor Teste");
        List<PagarParcela> parcelasFiltradas = parcelas.subList(0, 1);
        Page<PagarParcela> paginaFiltrada = new PageImpl<>(parcelasFiltradas, pageable, parcelasFiltradas.size());

        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaFiltrada);

        mockMvc.perform(get("/pagar")
                .param("fornecedor", "Fornecedor Teste"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", hasSize(1)));
    }

    @Test
    @DisplayName("Deve filtrar parcelas por fornecedor com isolamento completo")
    void deveListarParcelasPorFornecedorComIsolamento() throws Exception {
        // Arrange
        String nomeFornecedor = "Fornecedor Teste";
        ArgumentCaptor<PagarParcelaFilter> filterCaptor = ArgumentCaptor.forClass(PagarParcelaFilter.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        
        List<PagarParcela> parcelasFiltradas = parcelas.subList(0, 1);
        Page<PagarParcela> paginaFiltrada = new PageImpl<>(parcelasFiltradas, pageable, parcelasFiltradas.size());

        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaFiltrada);

        // Act
        mockMvc.perform(get("/pagar")
                .param("fornecedor", nomeFornecedor))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", hasSize(1)));

        // Verify - Capturar e verificar os argumentos exatos que foram passados
        verify(pagarParcelaService, times(1)).lista(
            filterCaptor.capture(), 
            pageableCaptor.capture()
        );
        
        // Verificar que o filtro foi configurado corretamente
        PagarParcelaFilter capturedFilter = filterCaptor.getValue();
        assertThat(capturedFilter.getNome(), equalTo(nomeFornecedor));
        
        // Verificar que a paginação foi configurada corretamente  
        Pageable capturedPageable = pageableCaptor.getValue();
        assertThat(capturedPageable.getPageNumber(), equalTo(0));
        assertThat(capturedPageable.getPageSize(), equalTo(10));
        
        // Garantir isolamento - outros serviços não foram chamados
        verifyNoMoreInteractions(pagarService);
        verifyNoMoreInteractions(fornecedorService);
        verifyNoMoreInteractions(pagarTipoService);
        verifyNoMoreInteractions(caixaService);
    }

    @Test
    @DisplayName("Deve listar parcelas vencidas")
    void deveListarParcelasVencidas() throws Exception {
        // Modificando uma parcela para estar vencida
        parcelas.get(0).setData_vencimento(LocalDate.now().minusDays(1));
        Page<PagarParcela> paginaVencidas = new PageImpl<>(parcelas.subList(0, 1), pageable, 1);

        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaVencidas);

        mockMvc.perform(get("/pagar")
                .param("situacao", "VENCIDA"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", hasSize(1)));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver parcelas")
    void deveRetornarListaVaziaQuandoNaoHouverParcelas() throws Exception {
        Page<PagarParcela> paginaVazia = new PageImpl<>(new ArrayList<>(), pageable, 0);

        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaVazia);

        mockMvc.perform(get("/pagar"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", empty()))
            .andExpect(model().attribute("qtdpaginas", is(0)))
            .andExpect(model().attribute("pagAtual", is(0)));
    }

    @Test
    @DisplayName("Deve paginar resultados corretamente")
    void devePaginarResultadosCorretamente() throws Exception {
        // Criando mais parcelas para testar paginação
        List<PagarParcela> muitasParcelas = new ArrayList<>();
        for(int i = 0; i < 25; i++) {
            PagarParcela parcela = new PagarParcela();
            parcela.setCodigo((long) (i + 1));
            parcela.setValor_total(100.0);
            muitasParcelas.add(parcela);
        }

        Pageable pageRequest = PageRequest.of(1, 10); // Segunda página
        Page<PagarParcela> paginaMuitasParcelas = new PageImpl<>(
            muitasParcelas.subList(10, 20), // Segunda página de resultados
            pageRequest,
            muitasParcelas.size()
        );

        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaMuitasParcelas);

        mockMvc.perform(get("/pagar")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(model().attribute("parcelas", hasSize(10)))
            .andExpect(model().attribute("qtdpaginas", is(3)))
            .andExpect(model().attribute("pagAtual", is(1)))
            .andExpect(model().attribute("hasNext", is(true)))
            .andExpect(model().attribute("hasPrevious", is(true)));
    }
	//RF2

    @Test
    @DisplayName("Deve registrar pagamento com sucesso quando dados estiverem corretos")
    void deveRegistrarPagamentoQuandoDadosCorretos() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";
        String expectedResponse = "Pagamento realizado com sucesso";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenReturn(expectedResponse);

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string(expectedResponse));

        // Verify - Garantir isolamento completo
        verify(pagarService, times(1)).quitar(
            eq(codParcela), 
            eq(50.0), 
            eq(0.0), 
            eq(0.0), 
            eq(codCaixa)
        );
        
        // Verificar que nenhum outro serviço foi chamado
        verifyNoMoreInteractions(pagarParcelaService);
        verifyNoMoreInteractions(fornecedorService);
        verifyNoMoreInteractions(pagarTipoService);
        verifyNoMoreInteractions(caixaService);
    }

    @Test
    @DisplayName("Deve registrar pagamento com desconto")
    void deveRegistrarPagamentoComDesconto() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "45,00";
        String desconto = "5,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 45.0, 5.0, 0.0, codCaixa))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve registrar pagamento com acréscimo")
    void deveRegistrarPagamentoComAcrescimo() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "2,50";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 2.5, codCaixa))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar pagamento sem informar parcela")
    void deveFalharAoRegistrarPagamentoSemParcela() throws Exception {
        mockMvc.perform(post("/pagar/quitar")
                .param("caixa", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar pagamento sem informar caixa")
    void deveFalharAoRegistrarPagamentoSemCaixa() throws Exception {
        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve falhar quando valor do pagamento for inválido")
    void deveFalharQuandoValorPagamentoInvalido() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String expectedErrorMessage = "Valor de pagamento inválido";

        // Configurar mock para simular exceção de negócio
        when(pagarService.quitar(codParcela, -50.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException(expectedErrorMessage));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", "-50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(expectedErrorMessage));

        // Verify - Verificar que apenas o serviço de pagamento foi chamado
        verify(pagarService, times(1)).quitar(
            eq(codParcela), 
            eq(-50.0), 
            eq(0.0), 
            eq(0.0), 
            eq(codCaixa)
        );
        
        // Verificar que outros serviços não foram afetados
        verifyNoMoreInteractions(pagarParcelaService);
        verifyNoMoreInteractions(fornecedorService);
        verifyNoMoreInteractions(pagarTipoService);
        verifyNoMoreInteractions(caixaService);
    }

    @Test
    @DisplayName("Deve converter corretamente valores vazios para zero")
    void deveConverterValoresVaziosParaZero() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", "50,00")
                .param("desconto", "")
                .param("acrescimo", ""))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }
	//RF3
	
    @Test
    @DisplayName("Deve falhar quando saldo do caixa for insuficiente")
    void deveFalharQuandoSaldoCaixaInsuficiente() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "1000,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 1000.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException("Saldo insuficiente para realizar este pagamento"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Saldo insuficiente para realizar este pagamento"));
    }

    @Test
    @DisplayName("Deve falhar quando parcela já estiver paga")
    void deveFalharQuandoParcelaJaPaga() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException("Parcela já foi paga"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Parcela já foi paga"));
    }

    @Test
    @DisplayName("Deve falhar quando parcela não existir")
    void deveFalharQuandoParcelaNaoExistir() throws Exception {
        Long codParcela = 999L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException("Parcela não encontrada"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Parcela não encontrada"));
    }

    @Test
    @DisplayName("Deve falhar quando valor total exceder valor da parcela")
    void deveFalharQuandoValorTotalExcederValorParcela() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "100,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 100.0, codCaixa))
            .thenThrow(new RuntimeException("Valor total não pode exceder o valor da parcela"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Valor total não pode exceder o valor da parcela"));
    }

    @Test
    @DisplayName("Deve falhar quando caixa estiver fechado")
    void deveFalharQuandoCaixaFechado() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException("Caixa está fechado"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Caixa está fechado"));
    }

    @Test
    @DisplayName("Deve validar formato dos valores monetários")
    void deveValidarFormatoValoresMonetarios() throws Exception {
        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("caixa", "1")
                .param("vlpago", "50.00") // Formato inválido, deve usar vírgula
                .param("desconto", "0,00")
                .param("acrescimo", "0,00"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Formato de valor inválido"));
    }
	//RF4

    @Test
    @DisplayName("Deve atualizar data de pagamento ao quitar parcela")
    void deveAtualizarDataPagamentoAoQuitarParcela() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        // Verifica se a parcela foi atualizada com a data de pagamento
        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenAnswer(invocation -> {
                PagarParcela parcela = parcelas.get(0);
                parcela.setData_pagamento(java.sql.Timestamp.valueOf(LocalDate.now().atStartOfDay()));
                return "Pagamento realizado com sucesso";
            });

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve criar lançamento no caixa ao processar pagamento")
    void deveCriarLancamentoCaixaAoProcessarPagamento() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        // Configura o mock para verificar a criação do lançamento no caixa
        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenAnswer(invocation -> {
                // Simula a criação do lançamento no caixa
                return "Pagamento realizado com sucesso";
            });

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve vincular lançamento do caixa com a parcela paga")
    void deveVincularLancamentoCaixaComParcela() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenAnswer(invocation -> {
                // Simula a vinculação entre lançamento e parcela
                return "Pagamento realizado com sucesso";
            });

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve reverter operação se houver erro no processamento")
    void deveReverterOperacaoSeHouverErro() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenThrow(new RuntimeException("Erro ao processar pagamento"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Erro ao processar pagamento"));
    }

    @Test
    @DisplayName("Deve registrar o usuário que realizou o pagamento")
    void deveRegistrarUsuarioQueRealizouPagamento() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenAnswer(invocation -> {
                // Simula o registro do usuário no lançamento
                return "Pagamento realizado com sucesso";
            });

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("Deve atualizar o saldo do caixa após o pagamento")
    void deveAtualizarSaldoCaixaAposPagamento() throws Exception {
        Long codParcela = 1L;
        Long codCaixa = 1L;
        String vlpago = "50,00";
        String desconto = "0,00";
        String acrescimo = "0,00";

        when(pagarService.quitar(codParcela, 50.0, 0.0, 0.0, codCaixa))
            .thenAnswer(invocation -> {
                // Simula a atualização do saldo do caixa
                return "Pagamento realizado com sucesso";
            });

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", codParcela.toString())
                .param("caixa", codCaixa.toString())
                .param("vlpago", vlpago)
                .param("desconto", desconto)
                .param("acrescimo", acrescimo))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }
	//RF5

    @Test
    @DisplayName("RF5.1 - Deve registrar uma transação de pagamento com sucesso")
    void deveRegistrarTransacaoPagamento() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("RF5.2 - Deve registrar pagamento com desconto e acréscimo")
    void deveRegistrarPagamentoComDescontoEAcrescimo() throws Exception {
        when(pagarService.quitar(eq(1L), eq(80.0), eq(10.0), eq(5.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "80,00")
                .param("desconto", "10,00")
                .param("acrescimo", "5,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("RF5.3 - Deve rejeitar pagamento quando caixa tem saldo insuficiente")
    void deveRejeitarPagamentoComSaldoInsuficiente() throws Exception {
        when(pagarService.quitar(eq(1L), eq(1000.0), eq(0.0), eq(0.0), eq(1L)))
            .thenThrow(new RuntimeException("Saldo insuficiente para realizar este pagamento"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "1000,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Saldo insuficiente para realizar este pagamento"));
    }

    @Test
    @DisplayName("RF5.4 - Deve rejeitar pagamento com valor maior que o valor restante")
    void deveRejeitarPagamentoComValorMaiorQueRestante() throws Exception {
        when(pagarService.quitar(eq(1L), eq(150.0), eq(0.0), eq(0.0), eq(1L)))
            .thenThrow(new RuntimeException("Valor de pagamento inválido"));

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "150,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Valor de pagamento inválido"));
    }

    @Test
    @DisplayName("RF5.5 - Deve validar campos obrigatórios no registro do pagamento")
    void deveValidarCamposObrigatorios() throws Exception {
        mockMvc.perform(post("/pagar/quitar"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "")
                .param("vlpago", "")
                .param("caixa", ""))
            .andExpect(status().isBadRequest());
    }
	//RF6

    @Test
    @DisplayName("RF6.1 - Deve retornar relatório de pagamentos por período")
    void deveRetornarRelatorioPagamentosPorPeriodo() throws Exception {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();

        mockMvc.perform(get("/pagar/relatorio")
                .param("dataInicio", inicio.toString())
                .param("dataFim", fim.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/relatorio"))
            .andExpect(model().attributeExists("pagamentos"))
            .andExpect(model().attributeExists("totalPago"));
    }

    @Test
    @DisplayName("RF6.2 - Deve retornar balanço financeiro por fornecedor")
    void deveRetornarBalancoPorFornecedor() throws Exception {
        Long fornecedorId = 1L;

        mockMvc.perform(get("/pagar/balanco-fornecedor")
                .param("fornecedorId", fornecedorId.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/balanco"))
            .andExpect(model().attributeExists("totalPago"))
            .andExpect(model().attributeExists("totalEmAberto"))
            .andExpect(model().attributeExists("fornecedor"));
    }

    @Test
    @DisplayName("RF6.3 - Deve gerar relatório de pagamentos em atraso")
    void deveGerarRelatorioPagamentosEmAtraso() throws Exception {
        mockMvc.perform(get("/pagar/relatorio-atrasos"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/relatorio-atrasos"))
            .andExpect(model().attributeExists("pagamentosAtrasados"))
            .andExpect(model().attributeExists("totalEmAtraso"))
            .andExpect(model().attributeExists("diasMediosAtraso"));
    }

    @Test
    @DisplayName("RF6.4 - Deve retornar estatísticas de pagamentos por tipo")
    void deveRetornarEstatisticasPorTipo() throws Exception {
        mockMvc.perform(get("/pagar/estatisticas-tipo"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/estatisticas"))
            .andExpect(model().attributeExists("estatisticasPorTipo"))
            .andExpect(model().attributeExists("totalGeral"));
    }

    @Test
    @DisplayName("RF6.5 - Deve validar período do relatório")
    void deveValidarPeriodoRelatorio() throws Exception {
        LocalDate dataInvalida = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/pagar/relatorio")
                .param("dataInicio", dataInvalida.toString())
                .param("dataFim", LocalDate.now().toString()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Data inicial não pode ser maior que data final")));
    }

    @Test
    @DisplayName("RF6.6 - Deve exportar relatório em formato PDF")
    void deveExportarRelatorioPDF() throws Exception {
        LocalDate inicio = LocalDate.now().minusDays(30);
        LocalDate fim = LocalDate.now();

        mockMvc.perform(get("/pagar/relatorio/pdf")
                .param("dataInicio", inicio.toString())
                .param("dataFim", fim.toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }
	//RF7

    @Test
    @DisplayName("RF7.1 - Deve permitir acesso à listagem para usuário com permissão")
    void devePermitirAcessoListagemComPermissao() throws Exception {
        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaParcelas);

        mockMvc.perform(get("/pagar")
                .header("Authorization", "Bearer TOKEN_VALIDO")
                .param("role", "ROLE_FINANCEIRO"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"));
    }

    @Test
    @DisplayName("RF7.2 - Deve negar acesso à listagem para usuário sem permissão")
    void deveNegarAcessoListagemSemPermissao() throws Exception {
        mockMvc.perform(get("/pagar")
                .header("Authorization", "Bearer TOKEN_INVALIDO"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF7.3 - Deve permitir quitação apenas para usuário financeiro")
    void devePermitirQuitacaoApenasFinanceiro() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .header("Authorization", "Bearer TOKEN_VALIDO")
                .param("role", "ROLE_FINANCEIRO")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));
    }

    @Test
    @DisplayName("RF7.4 - Deve negar quitação para usuário sem permissão financeira")
    void deveNegarQuitacaoSemPermissaoFinanceira() throws Exception {
        mockMvc.perform(post("/pagar/quitar")
                .header("Authorization", "Bearer TOKEN_VALIDO")
                .param("role", "ROLE_VENDEDOR")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("caixa", "1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF7.5 - Deve registrar usuário que realizou o pagamento")
    void deveRegistrarUsuarioQuePagou() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .header("Authorization", "Bearer TOKEN_VALIDO")
                .param("role", "ROLE_FINANCEIRO")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1")
                .param("usuario", "usuario.teste"))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"));

        // Verifica se o serviço foi chamado com o usuário correto
        Mockito.verify(pagarService).quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L));
    }
	//RF8

    @Test
    @DisplayName("RF8.1 - Deve registrar log de consulta de pagamentos")
    void deveRegistrarLogConsultaPagamentos() throws Exception {
        when(pagarParcelaService.lista(any(PagarParcelaFilter.class), any(Pageable.class)))
            .thenReturn(paginaParcelas);

        mockMvc.perform(get("/pagar")
                .header("X-User-Id", "1")
                .header("X-Request-ID", "request-123"))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/list"))
            .andExpect(request().attribute("logOperacao", notNullValue()))
            .andExpect(request().attribute("dataOperacao", notNullValue()));
    }

    @Test
    @DisplayName("RF8.2 - Deve registrar detalhes da operação de pagamento")
    void deveRegistrarDetalhesOperacaoPagamento() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .header("X-User-Id", "1")
                .header("X-Request-ID", "request-123")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(request().attribute("logOperacao", containsString("Quitação de parcela")))
            .andExpect(request().attribute("valorOperacao", is(50.0)))
            .andExpect(request().attribute("parcelaId", is(1L)));
    }

    @Test
    @DisplayName("RF8.3 - Deve registrar tentativas de operações não autorizadas")
    void deveRegistrarTentativasNaoAutorizadas() throws Exception {
        mockMvc.perform(post("/pagar/quitar")
                .header("X-User-Id", "2")
                .header("X-Request-ID", "request-123")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("caixa", "1"))
            .andExpect(status().isForbidden())
            .andExpect(request().attribute("logOperacao", containsString("Tentativa não autorizada")))
            .andExpect(request().attribute("usuarioId", is("2")));
    }

    @Test
    @DisplayName("RF8.4 - Deve registrar alterações nos valores de pagamento")
    void deveRegistrarAlteracoesValores() throws Exception {
        when(pagarService.quitar(eq(1L), eq(45.0), eq(5.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .header("X-User-Id", "1")
                .header("X-Request-ID", "request-123")
                .param("parcela", "1")
                .param("vlpago", "45,00")
                .param("desconto", "5,00")
                .param("caixa", "1"))
            .andExpect(status().isOk())
            .andExpect(request().attribute("logOperacao", containsString("Alteração de valores")))
            .andExpect(request().attribute("valorOriginal", is(50.0)))
            .andExpect(request().attribute("valorFinal", is(45.0)))
            .andExpect(request().attribute("desconto", is(5.0)));
    }

    @Test
    @DisplayName("RF8.5 - Deve permitir consulta ao histórico de operações")
    void devePermitirConsultaHistoricoOperacoes() throws Exception {
        mockMvc.perform(get("/pagar/historico")
                .header("X-User-Id", "1")
                .param("dataInicio", LocalDate.now().minusDays(30).toString())
                .param("dataFim", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("pagar/historico"))
            .andExpect(model().attributeExists("operacoes"))
            .andExpect(model().attributeExists("totalOperacoes"));
    }

    @Test
    @DisplayName("RF8.6 - Deve registrar operações de alteração de vencimento")
    void deveRegistrarAlteracaoVencimento() throws Exception {
        LocalDate novoVencimento = LocalDate.now().plusDays(30);
        
        mockMvc.perform(post("/pagar/alterar-vencimento")
                .header("X-User-Id", "1")
                .header("X-Request-ID", "request-123")
                .param("parcela", "1")
                .param("novoVencimento", novoVencimento.toString()))
            .andExpect(status().isOk())
            .andExpect(request().attribute("logOperacao", containsString("Alteração de vencimento")))
            .andExpect(request().attribute("vencimentoOriginal", notNullValue()))
            .andExpect(request().attribute("vencimentoNovo", is(novoVencimento)));
    }
	//RF9

    @Test
    @DisplayName("RF9.1 - Deve enviar notificação após registro de pagamento")
    void deveEnviarNotificacaoAposRegistro() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1")
                .param("notificar", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string("Pagamento realizado com sucesso"))
            .andExpect(request().attribute("notificacaoEnviada", is(true)));
    }

    @Test
    @DisplayName("RF9.2 - Deve integrar com sistema contábil externo")
    void deveIntegrarSistemaContabil() throws Exception {
        when(pagarService.quitar(eq(1L), eq(50.0), eq(0.0), eq(0.0), eq(1L)))
            .thenReturn("Pagamento realizado com sucesso");

        mockMvc.perform(post("/pagar/quitar")
                .param("parcela", "1")
                .param("vlpago", "50,00")
                .param("desconto", "0,00")
                .param("acrescimo", "0,00")
                .param("caixa", "1")
                .param("integrarContabil", "true"))
            .andExpect(status().isOk())
            .andExpect(request().attribute("integracaoContabil", is(true)))
            .andExpect(request().attribute("lancamentoContabil", notNullValue()));
    }

    @Test
    @DisplayName("RF9.3 - Deve gerar arquivo de remessa bancária")
    void deveGerarArquivoRemessa() throws Exception {
        mockMvc.perform(post("/pagar/gerar-remessa")
                .param("banco", "001")
                .param("pagamentos", "1,2,3"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/octet-stream"))
            .andExpect(header().string("Content-Disposition", containsString("remessa")));
    }

    @Test
    @DisplayName("RF9.4 - Deve processar retorno bancário")
    void deveProcessarRetornoBancario() throws Exception {
        mockMvc.perform(post("/pagar/processar-retorno")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("banco", "001"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("processado com sucesso")))
            .andExpect(request().attribute("pagamentosProcessados", notNullValue()));
    }

    @Test
    @DisplayName("RF9.5 - Deve sincronizar com sistema ERP")
    void deveSincronizarComERP() throws Exception {
        mockMvc.perform(post("/pagar/sincronizar-erp")
                .param("dataInicio", LocalDate.now().minusDays(30).toString())
                .param("dataFim", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("sincronização concluída")))
            .andExpect(request().attribute("registrosSincronizados", notNullValue()));
    }

    @Test
    @DisplayName("RF9.6 - Deve validar consistência dos dados na integração")
    void deveValidarConsistenciaDadosIntegracao() throws Exception {
        mockMvc.perform(post("/pagar/validar-integracao")
                .param("sistema", "ERP")
                .param("dataReferencia", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("validação concluída")))
            .andExpect(request().attribute("inconsistencias", notNullValue()));
    }
	//RF10

    @Test
    @DisplayName("RF10.1 - Deve gerar backup de pagamentos por período")
    void deveGerarBackupPagamentosPeriodo() throws Exception {
        mockMvc.perform(get("/pagar/backup")
                .param("dataInicio", LocalDate.now().minusMonths(1).toString())
                .param("dataFim", LocalDate.now().toString())
                .param("formato", "JSON"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.pagamentos", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.dataBackup").exists())
            .andExpect(header().string("Content-Disposition", containsString("backup_pagamentos")));
    }

    @Test
    @DisplayName("RF10.2 - Deve restaurar dados de pagamento a partir de backup")
    void deveRestaurarDadosPagamento() throws Exception {
        mockMvc.perform(post("/pagar/restaurar")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("confirmacao", "true")
                .param("modo", "APPEND"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("restauração concluída")))
            .andExpect(request().attribute("registrosRestaurados", notNullValue()));
    }

    @Test
    @DisplayName("RF10.3 - Deve validar integridade dos dados no backup")
    void deveValidarIntegridadeBackup() throws Exception {
        mockMvc.perform(post("/pagar/validar-backup")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.integridadeOk").value(true))
            .andExpect(jsonPath("$.totalRegistros").exists())
            .andExpect(jsonPath("$.hashVerificacao").exists());
    }

    @Test
    @DisplayName("RF10.4 - Deve realizar backup incremental")
    void deveRealizarBackupIncremental() throws Exception {
        mockMvc.perform(get("/pagar/backup-incremental")
                .param("ultimoBackup", LocalDate.now().minusDays(1).toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.tipo").value("INCREMENTAL"))
            .andExpect(jsonPath("$.alteracoes").exists())
            .andExpect(jsonPath("$.dataBackupBase").exists());
    }

    @Test
    @DisplayName("RF10.5 - Deve rejeitar restauração sem confirmação")
    void deveRejeitarRestauracaoSemConfirmacao() throws Exception {
        mockMvc.perform(post("/pagar/restaurar")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .param("confirmacao", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("confirmação necessária")));
    }

    @Test
    @DisplayName("RF10.6 - Deve manter histórico de backups realizados")
    void deveManterHistoricoBackups() throws Exception {
        mockMvc.perform(get("/pagar/historico-backups")
                .param("periodo", "ULTIMOS_30_DIAS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backups").isArray())
            .andExpect(jsonPath("$.backups[*].data").exists())
            .andExpect(jsonPath("$.backups[*].tipo").exists())
            .andExpect(jsonPath("$.backups[*].tamanho").exists())
            .andExpect(jsonPath("$.backups[*].registros").exists());
    }
}

