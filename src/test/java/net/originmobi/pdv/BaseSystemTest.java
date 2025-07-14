package net.originmobi.pdv;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public abstract class BaseSystemTest {

    protected static WebDriver driver;

    @BeforeAll
    public static void setUp() {
        // O WebDriverManager baixa e configura o driver do Chrome automaticamente.
        WebDriverManager.chromedriver().setup();

        // Opcional: Executa o Chrome em modo headless (sem abrir uma janela de UI)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        driver = new ChromeDriver(options);

        // Define uma espera implícita. O Selenium aguardará até 10 segundos para os elementos aparecerem.
        driver.manage().timeouts().implicitlyWait(10L,TimeUnit.SECONDS);
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}