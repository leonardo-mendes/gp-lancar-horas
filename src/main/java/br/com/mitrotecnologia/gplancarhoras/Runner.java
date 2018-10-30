package br.com.mitrotecnologia.gplancarhoras;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author vinicius_brito
 * @version $Revision: $<br/>
 *          $Id: $
 * @since 10/30/18 12:42 PM
 */
@Component
public class Runner {

    private static final String CHROME_DRIVER_NAME = "webdriver.chrome.driver";

    @Value("${url_gp}")
    private String url;
    @Value("${driver_path}")
    private String driverPath;
    @Value("${user}")
    private String user;
    @Value("${pass}")
    private String pass;
    @Value("${app_name}")
    private String appName;
    @Value("${worked_hours}")
    private String workedHours;
    @Value("${task}")
    private String task;
    @Value("${start_date}")
    private String startDate;
    @Value("${end_date}")
    private String endDate;

    public void run() {
        try {
            WebDriver driver = getWebDriver();
            doLogin(driver);
            inputHours(driver);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private void inputHours(final WebDriver driver) {
        driver.navigate().to("https://helpdesk.tqi.com.br/tqiextranet/helpdesk/atividades.asp?TelaOrigem=menu");
        List<LocalDate> dates = getDatesBetween(LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd/MM/yyy")),
                LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd/MM/yyy")));
        for (LocalDate date : dates) {
            driver.findElement(By.id("b1")).click();
            driver.findElement(By.id("monthSelect")).click();
            String month = String.format("monthDiv_%s", date.getMonthValue() - 1);
            String year = String.format("yearDiv%s", date.getYear()-1);
            driver.findElement(By.id(month)).click();
            driver.findElement(By.xpath("//*[@id=\"topBar\"]/div[3]")).click();
            driver.findElement(By.id(year)).click();
            List<WebElement> elements = driver
                    .findElements(By.xpath("//*[@id=\"calendarDiv\"]/div[7]/table/tbody"));
            WebElement t = elements.get(0)
                    .findElements(By.tagName("td"))
                    .stream().filter(f -> f.getText().equals(String.valueOf(date.getDayOfMonth())))
                    .findFirst().get();
            t.click();
            driver.findElement(By.name("CmbAtividade")).click();
            driver.findElement(By.xpath("/html/body/div[10]/form/table[1]/tbody/tr[3]/td[2]/select/option[14]"))
                    .click();
            driver.findElement(By.name("DesAplicativo")).sendKeys(appName);
            driver.findElement(By.name("horas_trab")).sendKeys(workedHours);
            driver.findElement(By.name("BtGravar")).click();
            // System.out.println("AAA");
        }
    }

    private WebDriver getWebDriver() {
        // create a Chrome Web Driver
        System.setProperty(CHROME_DRIVER_NAME, driverPath);
        WebDriver driver = new ChromeDriver();
        driver.get(url);
        return driver;
    }

    private void doLogin(final WebDriver driver) throws InterruptedException {
        driver.findElement(By.id("userName")).sendKeys(user.replaceAll("_", "."));
        driver.findElement(By.id("userPass")).sendKeys(pass);
        driver.findElement(By.id("submitLogin")).click();
        while (!driver.getCurrentUrl().contains("https://helpdesk.tqi.com.br/tqiextranet/extranet.asp")) {
            Thread.sleep(2000);
        }
    }

    private List<LocalDate> getDatesBetween(
            LocalDate startDate, LocalDate endDate) {

        long numOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (numOfDaysBetween > 0) {
            return IntStream.iterate(0, i -> i + 1)
                    .limit(numOfDaysBetween)
                    .mapToObj(i -> startDate.plusDays(i))
                    .collect(Collectors.toList());
        } else {
            return Arrays.asList(startDate);
        }

    }
}