package net.originmobi.pdv;

import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import   static  org.junit.jupiter.api.Assertions.assertEquals;

public class SistemaTest extends BaseSystemTest {

    private static final String APP_BASE_URL = "http://localhost:8080";

    @BeforeEach
    public void navigateToLoginPage() {
        driver.get(APP_BASE_URL + "/login");
    }

    /**
     * Teste Funcional: Verifica se um usuário com credenciais válidas pode fazer login.
     *
     * Pré-requisitos:
     * - A página de login está em "/login".
     * - Os campos de input têm os atributos 'name' como "username" e "password".
     * - Um login bem-sucedido redireciona para a página inicial ("/").
     * - A página inicial contém o texto "Pedidos em Aberto".
     */
    @Test
    public void testSuccessfulLogin() {
        // Encontra os elementos do formulário
        WebElement usernameInput = driver.findElement(By.name("username"));
        WebElement passwordInput = driver.findElement(By.name("password"));
        WebElement submitButton = driver.findElement(By.tagName("button"));

        // Insere as credenciais (substitua por um usuário de teste real)
        usernameInput.sendKeys("admin");
        passwordInput.sendKeys("admin");

        // Submete o formulário
        submitButton.click();

        // Espera o redirecionamento para o dashboard e a visibilidade de um elemento conhecido
        WebDriverWait wait = new WebDriverWait(driver,Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlToBe(APP_BASE_URL + "/"));

        // Valida se o login foi bem-sucedido verificando a URL e o conteúdo da página
        Assertions.assertEquals("Deveria ser redirecionado para a página inicial", APP_BASE_URL + "/", driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains("Pedidos em Aberto"),
                "A página inicial deve conter o painel 'Pedidos em Aberto'");
    }

    /**
     * Teste Não Funcional: Verifica se a página de login carrega dentro de um tempo aceitável.
     *
     * Este teste mede a performance de carregamento da página, um requisito não funcional.
     */
    @Test
    public void testLoginPageLoadTime() {
        long startTime = System.currentTimeMillis();

        // Navega para a página de login
        driver.get(APP_BASE_URL + "/login");

        long endTime = System.currentTimeMillis();
        long loadTime = endTime - startTime;

        // Define um tempo de carregamento máximo aceitável em milissegundos (ex: 2000ms = 2 segundos)
        long maxLoadTime = 2000;

        System.out.println("O tempo de carregamento da página foi de " + loadTime + " ms.");
        assertTrue(loadTime < maxLoadTime, "A página deveria carregar em menos de " + maxLoadTime + " ms.");
    }
}