package net.originmobi.pdv.selenium;

import net.originmobi.pdv.BaseSystemTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

public class CaixaSystemTest extends BaseSystemTest {

    private static final String APP_BASE_URL = "http://localhost:8080";


    public void loginAndNavigate() {
        // Garante que o usuário esteja logado
        driver.get(APP_BASE_URL + "/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("admin");
        driver.findElement(By.tagName("button")).click();

        // Navega para a página de gerenciamento do caixa
        driver.get(APP_BASE_URL + "/caixa");
    }

    @Test
    public void testAbrirCaixaComSucesso() {

        // 1. Localize e clique no botão "Abrir Caixa"

        WebElement abrirCaixaBtn = driver.findElement(By.id("abrir-caixa-btn"));
        abrirCaixaBtn.click();

        // 2. Aguarde o modal/formulário de abertura aparecer
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modal-abrir-caixa")));

        // 3. Preencha o valor inicial do caixa (suprimento)
        WebElement valorInicialInput = driver.findElement(By.id("valor-inicial"));
        valorInicialInput.sendKeys("100.00");

        // 4. Confirme a abertura

        WebElement confirmarBtn = driver.findElement(By.id("confirmar-abertura-btn"));
        confirmarBtn.click();

        // 5. Valide o resultado
        // Verifique se uma mensagem de sucesso apareceu ou se o status do caixa na página mudou.
        WebElement statusCaixa = driver.findElement(By.id("status-caixa"));
        assertEquals("O status do caixa deveria ser 'ABERTO'", "ABERTO", statusCaixa.getText());

        // --- FIM DA LÓGICA DE TESTE ---
    }
}