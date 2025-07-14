package net.originmobi.pdv.selenium;

import net.originmobi.pdv.BaseSystemTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

public class EmpresaSystemTest extends BaseSystemTest {

    private static final String APP_BASE_URL = "http://localhost:8080";

    public void loginAndNavigate() {

        driver.get(APP_BASE_URL + "/login");
        driver.findElement(By.name("username")).sendKeys("admin");
        driver.findElement(By.name("password")).sendKeys("admin");
        driver.findElement(By.tagName("button")).click();

        // Navega para a página de cadastro da empresa
        // Assumindo que a URL seja /empresa. Ajuste se for diferente.
        driver.get(APP_BASE_URL + "/empresa");
    }

    @Test
    public void testSalvarDadosDaEmpresaComSucesso() {

        WebElement nomeInput = driver.findElement(By.name("nome"));
        WebElement nomeFantasiaInput = driver.findElement(By.name("nome_fantasia"));
        WebElement cnpjInput = driver.findElement(By.name("cnpj"));
        WebElement ieInput = driver.findElement(By.name("ie"));
        WebElement ruaInput = driver.findElement(By.name("rua"));
        WebElement bairroInput = driver.findElement(By.name("bairro"));
        WebElement numeroInput = driver.findElement(By.name("numero"));
        WebElement cepInput = driver.findElement(By.name("cep"));
        WebElement submitButton = driver.findElement(By.id("salvar-empresa-btn")); // Supondo que o botão tenha um ID

        nomeInput.clear();
        nomeFantasiaInput.clear();
        cnpjInput.clear();

        nomeInput.sendKeys("Minha Empresa Teste");
        nomeFantasiaInput.sendKeys("Empresa Teste Fantasia");
        cnpjInput.sendKeys("12345678000199"); // Use um CNPJ válido para teste
        ieInput.sendKeys("123456789");
        ruaInput.sendKeys("Rua do Teste");
        bairroInput.sendKeys("Bairro da Simulação");
        numeroInput.sendKeys("123");
        cepInput.sendKeys("12345-678");

        Select regimeSelect = new Select(driver.findElement(By.name("codRegime")));
        regimeSelect.selectByValue("1"); // Supondo que o valor '1' exista

        submitButton.click();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement alert = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("alert-success")));

        // Valida se a mensagem de sucesso é a esperada
        assertTrue("A mensagem de sucesso deve ser exibida", alert.isDisplayed());
        assertEquals("A mensagem de sucesso está incorreta", "Empresa salva com sucesso", alert.getText());
    }
}