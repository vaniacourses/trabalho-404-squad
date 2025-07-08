package net.originmobi.pdv.selenium;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Testes de Sistema com Selenium para módulo de Pagamentos.
 * 
 * Verifica o funcionamento da aplicação através da interface web,
 * simulando interações reais do usuário.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class PagamentoSystemTest {

    private static final Logger logger = Logger.getLogger(PagamentoSystemTest.class.getName());
    
    // Constantes de timeout e configuração
    private static final long TIMEOUT_SEGUNDOS = 10;
    private static final String BASE_URL = "http://localhost:%d";
    
    // Constantes de URLs e endpoints
    private static final String PAGE_LOGIN = "/login";
    private static final String PAGE_PAGAR = "/pagar";
    private static final String PAGE_HOME = "/home";
    
    // Constantes de seletores
    private static final String SELECTOR_TABELA_DESPESAS = ".tab-despesas";
    private static final String SELECTOR_BTN_NOVO = ".btnAbreModal";
    private static final String SELECTOR_MODAL_DESPESA = ".modalDespesa";
    private static final String SELECTOR_MODAL_PAGAMENTO = ".modalpagdespesa";
    
    // Constantes de IDs de elementos
    private static final String ID_FORNECEDOR = "codFornecedor";
    private static final String ID_VALOR_TOTAL = "vltotalDespesa";
    private static final String ID_TIPO_DESPESA = "despesatipo";
    private static final String ID_DATA_VENCIMENTO = "dataVencimento";
    private static final String ID_OBS = "obs";
    private static final String ID_VALOR_PAGO = "valorpago";
    private static final String ID_CAIXA = "caixa";
    
    // Constantes de mensagens
    private static final String MSG_SUCESSO = "sucesso";
    private static final String MSG_CAMPOS_OBRIGATORIOS = "Preencha os campos obrigatórios";
    private static final String MSG_VALOR_INVALIDO = "valor";
    
    // Constantes de dados de teste
    private static final String VALOR_DESPESA = "150,00";
    private static final String DATA_VENCIMENTO = "31/12/2023";
    private static final String OBS_TESTE = "Despesa de teste automatizado";

    @LocalServerPort
    private int port;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        logger.info("Iniciando teste de sistema");
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        wait = new WebDriverWait(driver, TIMEOUT_SEGUNDOS);
        fazerLogin();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            logger.info("Finalizando teste e fechando navegador");
            driver.quit();
        }
    }

    private void fazerLogin() {
        logger.info("Realizando login no sistema");
        driver.get(String.format(BASE_URL + PAGE_LOGIN, port));
        driver.findElement(By.id("user")).sendKeys("teste");
        driver.findElement(By.id("password")).sendKeys("123");
        driver.findElement(By.id("btn-login")).click();
        wait.until(ExpectedConditions.urlContains(PAGE_HOME));
    }

    private void aguardarAtualizacaoTabela() {
        wait.until((ExpectedCondition<Boolean>) driver -> {
            try {
                List<WebElement> linhas = driver.findElements(By.cssSelector(SELECTOR_TABELA_DESPESAS + " tbody tr"));
                return !linhas.isEmpty() || driver.findElement(By.cssSelector(SELECTOR_TABELA_DESPESAS + " tbody")).getText().trim().isEmpty();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private Alert aguardarEObterAlerta() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    private void validarEFecharAlerta(String mensagemEsperada, boolean esperaSucesso) {
        Alert alert = aguardarEObterAlerta();
        String texto = alert.getText();
        if (esperaSucesso) {
            assertTrue(texto.contains(mensagemEsperada), 
                String.format("Alerta deve conter '%s', mas contém '%s'", mensagemEsperada, texto));
        } else {
            assertFalse(texto.isEmpty(), "Alerta não deve estar vazio");
            if (mensagemEsperada != null) {
                assertTrue(texto.contains(mensagemEsperada), 
                    String.format("Alerta deve conter '%s', mas contém '%s'", mensagemEsperada, texto));
            }
        }
        alert.accept();
    }

    private void navegarParaPaginaPagamentos() {
        logger.info("Navegando para página de pagamentos");
        driver.get(String.format(BASE_URL + PAGE_PAGAR, port));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(SELECTOR_TABELA_DESPESAS)));
    }

    @Test
    @DisplayName("SISTEMA: Deve listar despesas corretamente")
    void deveListarDespesasCorretamente() {
        // Navegar para página de pagamentos
        navegarParaPaginaPagamentos();
        
        // Verificar título da página
        WebElement titulo = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("titulo-h1")));
        assertEquals("Despesas", titulo.getText(), "Título deve ser 'Despesas'");
        
        // Verificar presença da tabela de despesas
        WebElement tabela = wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tab-despesas")));
        assertNotNull(tabela, "Tabela de despesas deve estar presente");
        
        // Verificar colunas da tabela
        List<WebElement> cabecalhos = tabela.findElements(By.tagName("th"));
        assertTrue(cabecalhos.size() >= 7, "Tabela deve ter pelo menos 7 colunas");
        assertEquals("#", cabecalhos.get(0).getText().trim());
        assertEquals("Fornecedor", cabecalhos.get(1).getText().trim());
        assertEquals("Observação", cabecalhos.get(2).getText().trim());
    }

    @Test
    @DisplayName("SISTEMA: Deve criar nova despesa com sucesso")
    void deveCriarNovaDespesaComSucesso() {
        // Navegar para página de despesas
        navegarParaPaginaPagamentos();
        
        // Clicar no botão Novo
        WebElement btnNovo = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btnAbreModal")));
        btnNovo.click();
        
        // Aguardar modal abrir
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("modalDespesa")));
        
        // Preencher formulário no modal
        WebElement selectFornecedor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("codFornecedor")));
        new Select(selectFornecedor).selectByIndex(1); // Seleciona primeiro fornecedor disponível
        
        WebElement inputValor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("vltotalDespesa")));
        inputValor.clear();
        inputValor.sendKeys(VALOR_DESPESA);
        
        WebElement selectTipo = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("despesatipo")));
        new Select(selectTipo).selectByIndex(1); // Seleciona primeiro tipo disponível
        
        // Preencher data de vencimento
        WebElement dataVenc = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("dataVencimento")));
        dataVenc.clear();
        dataVenc.sendKeys(DATA_VENCIMENTO);
        
        // Preencher observação
        WebElement obs = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("obs")));
        obs.sendKeys(OBS_TESTE);
        
        // Clicar no botão Lançar
        WebElement btnLancar = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-despesa")));
        btnLancar.click();
        
        // Validar mensagem de sucesso
        validarEFecharAlerta(MSG_SUCESSO, true);
        
        // Verificar que modal fechou e tabela atualizou
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("modalDespesa")));
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//td[normalize-space(text())='Despesa de teste automatizado']")));
    }

    @Test
    @DisplayName("SISTEMA: Deve validar campos obrigatórios ao criar despesa")
    void deveValidarCamposObrigatoriosAoCriarDespesa() {
        // Navegar para página de despesas
        navegarParaPaginaPagamentos();
        
        // Abrir modal de nova despesa
        WebElement btnNovo = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".btnAbreModal")));
        btnNovo.click();
        
        // Aguardar modal abrir
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("modalDespesa")));
        
        // Tentar lançar sem preencher nada
        WebElement btnLancar = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-despesa")));
        btnLancar.click();
        
        // Validar mensagem de erro
        validarEFecharAlerta(MSG_CAMPOS_OBRIGATORIOS, false);
        
        // Modal deve continuar aberto
        assertTrue(driver.findElement(By.className("modalDespesa")).isDisplayed(), 
            "Modal deve permanecer aberto após erro de validação");
    }
    
    @Test
    @DisplayName("SISTEMA: Deve filtrar despesas corretamente")
    void deveFiltrarDespesasCorretamente() {
        // Navegar para página de despesas
        navegarParaPaginaPagamentos();
        
        // Localizar e preencher campo de busca
        WebElement filtro = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("busca")));
        filtro.clear();
        filtro.sendKeys("teste");
        
        // Clicar no botão de busca
        WebElement btnBusca = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-busca-pagar")));
        btnBusca.click();
        
        // Aguardar atualização da tabela
        aguardarAtualizacaoTabela();
        
        // Verificar resultados
        List<WebElement> linhasTabela = driver.findElements(By.cssSelector(".tab-despesas tbody tr"));
        assertTrue(linhasTabela.size() >= 0, "Deve exibir resultados filtrados ou tabela vazia");
        
        // Se houver resultados, verificar que contêm o termo buscado
        if (!linhasTabela.isEmpty()) {
            boolean encontrouTermo = linhasTabela.stream()
                .anyMatch(tr -> tr.getText().toLowerCase().contains("teste"));
            assertTrue(encontrouTermo, "Resultados devem conter o termo buscado");
        }
    }
    
    @Test
    @DisplayName("SISTEMA: Deve realizar pagamento de despesa com sucesso")
    void deveRealizarPagamentoDeDespesaComSucesso() {
        // Primeiro criar uma despesa de teste
        deveCriarNovaDespesaComSucesso();
        
        // Localizar e clicar no botão de pagamento da despesa recém-criada
        WebElement btnPagar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//td[normalize-space(text())='Despesa de teste automatizado']/..//a[contains(@class,'btn-modal-paga')]")));
        btnPagar.click();
        
        // Aguardar modal de pagamento
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("modalpagdespesa")));
        
        // Preencher valor do pagamento (que já deve vir preenchido automaticamente)
        WebElement valorPago = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("valorpago")));
        assertTrue(!valorPago.getAttribute("value").isEmpty(), "Valor a pagar deve vir preenchido");
        
        // Selecionar caixa
        WebElement selectCaixa = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("caixa")));
        new Select(selectCaixa).selectByIndex(0); // Seleciona primeiro caixa disponível
        
        // Clicar no botão Pagar
        WebElement btnConfirmarPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-pag-despesa")));
        btnConfirmarPagamento.click();
        
        // Validar mensagem de sucesso
        validarEFecharAlerta(MSG_SUCESSO, true);
        
        // Verificar que modal fechou e tabela atualizou
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("modalpagdespesa")));
        
        // Verificar que a despesa foi marcada como quitada (classe warning na tr)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath("//td[normalize-space(text())='Despesa de teste automatizado']/../@class[contains(.,'warning')]")));
    }

    @Test
    @DisplayName("SISTEMA: Deve validar valor mínimo ao pagar despesa")
    void deveValidarValorMinimoAoPagarDespesa() {
        // Primeiro criar uma despesa de teste
        deveCriarNovaDespesaComSucesso();
        
        // Localizar e clicar no botão de pagamento da despesa recém-criada
        WebElement btnPagar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//td[normalize-space(text())='Despesa de teste automatizado']/..//a[contains(@class,'btn-modal-paga')]")));
        btnPagar.click();
        
        // Aguardar modal de pagamento
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("modalpagdespesa")));
        
        // Tentar pagar com valor zero
        WebElement valorPago = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("valorpago")));
        valorPago.clear();
        valorPago.sendKeys("0,00");
        
        // Selecionar caixa
        WebElement selectCaixa = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("caixa")));
        new Select(selectCaixa).selectByIndex(0);
        
        // Tentar pagar
        WebElement btnConfirmarPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-pag-despesa")));
        btnConfirmarPagamento.click();
        
        // Validar mensagem de erro
        validarEFecharAlerta(MSG_VALOR_INVALIDO, false);
        
        // Modal deve continuar aberto
        assertTrue(driver.findElement(By.className("modalpagdespesa")).isDisplayed(), 
            "Modal deve permanecer aberto após erro de validação");
    }
    
    @Test
    @DisplayName("SISTEMA: Deve realizar pagamento parcial de despesa")
    void deveRealizarPagamentoParcialDeDespesa() {
        // Primeiro criar uma despesa de teste
        deveCriarNovaDespesaComSucesso();
        
        // Localizar e clicar no botão de pagamento
        WebElement btnPagar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath(String.format("//td[normalize-space(text())='%s']/..//a[contains(@class,'btn-modal-paga')]", OBS_TESTE))));
        btnPagar.click();
        
        // Aguardar modal de pagamento
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(SELECTOR_MODAL_PAGAMENTO)));
        
        // Alterar para pagamento parcial
        WebElement valorPago = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_VALOR_PAGO)));
        valorPago.clear();
        valorPago.sendKeys("75,00"); // Metade do valor total
        
        // Selecionar caixa
        WebElement selectCaixa = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_CAIXA)));
        new Select(selectCaixa).selectByIndex(0);
        
        // Confirmar pagamento
        WebElement btnConfirmarPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-pag-despesa")));
        btnConfirmarPagamento.click();
        
        // Validar sucesso
        validarEFecharAlerta(MSG_SUCESSO, true);
        
        // Verificar que modal fechou
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(SELECTOR_MODAL_PAGAMENTO)));
        
        // Verificar que a despesa ainda está pendente (não tem classe warning)
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.xpath(String.format("//td[normalize-space(text())='%s']/../not(@class='warning')", OBS_TESTE))));
    }
    
    @Test
    @DisplayName("SISTEMA: Deve validar seleção de caixa ao pagar despesa")
    void deveValidarSelecaoDeCaixaAoPagarDespesa() {
        // Primeiro criar uma despesa de teste
        deveCriarNovaDespesaComSucesso();
        
        // Localizar e clicar no botão de pagamento
        WebElement btnPagar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath(String.format("//td[normalize-space(text())='%s']/..//a[contains(@class,'btn-modal-paga')]", OBS_TESTE))));
        btnPagar.click();
        
        // Aguardar modal de pagamento
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(SELECTOR_MODAL_PAGAMENTO)));
        
        // Tentar pagar sem selecionar caixa
        WebElement btnConfirmarPagamento = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-pag-despesa")));
        btnConfirmarPagamento.click();
        
        // Validar erro
        validarEFecharAlerta("Selecione um caixa", false);
        
        // Modal deve continuar aberto
        assertTrue(driver.findElement(By.cssSelector(SELECTOR_MODAL_PAGAMENTO)).isDisplayed(), 
            "Modal deve permanecer aberto após erro de validação");
    }

    @Test
    @DisplayName("SISTEMA: Deve validar data de vencimento ao criar despesa")
    void deveValidarDataVencimentoAoCriarDespesa() {
        // Navegar para página de despesas
        navegarParaPaginaPagamentos();
        
        // Abrir modal de nova despesa
        WebElement btnNovo = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(SELECTOR_BTN_NOVO)));
        btnNovo.click();
        
        // Aguardar modal
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(SELECTOR_MODAL_DESPESA)));
        
        // Preencher dados básicos
        WebElement selectFornecedor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_FORNECEDOR)));
        new Select(selectFornecedor).selectByIndex(1);
        
        WebElement inputValor = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_VALOR_TOTAL)));
        inputValor.sendKeys(VALOR_DESPESA);
        
        WebElement selectTipo = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_TIPO_DESPESA)));
        new Select(selectTipo).selectByIndex(1);
        
        // Tentar data inválida
        WebElement dataVenc = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(ID_DATA_VENCIMENTO)));
        dataVenc.sendKeys("99/99/9999");
        
        // Tentar lançar
        WebElement btnLancar = wait.until(ExpectedConditions.elementToBeClickable(By.className("btn-despesa")));
        btnLancar.click();
        
        // Validar erro
        validarEFecharAlerta("data", false);
        
        // Modal deve continuar aberto
        assertTrue(driver.findElement(By.cssSelector(SELECTOR_MODAL_DESPESA)).isDisplayed(), 
            "Modal deve permanecer aberto após erro de validação");
    }
}
