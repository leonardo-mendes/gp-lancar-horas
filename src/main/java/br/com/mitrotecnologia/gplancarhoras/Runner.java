package br.com.mitrotecnologia.gplancarhoras;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author vinicius_brito
 * @version $Revision: $<br/>
 * $Id: $
 * @since 10/30/18 12:42 PM
 */
@Component
public class Runner {

    private static final int SATURDAY = 6;
    private static final int SUNDAY = 7;

    private static final Set<Integer> WEEKEND;
    private static final Set<LocalDate> HOLIDAYS;

    static {
        List<Integer> dates = Arrays.asList(SATURDAY, SUNDAY);
        WEEKEND = Collections.unmodifiableSet(new HashSet<>(dates));
    }

    static {
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(2019, 1, 1),
                LocalDate.of(2019, 3, 30),
                LocalDate.of(2019, 4, 19),
                LocalDate.of(2019, 4, 21),
                LocalDate.of(2019, 5, 1),
                LocalDate.of(2019, 5, 31),
                LocalDate.of(2019, 8, 15),
                LocalDate.of(2019, 8, 31),
                LocalDate.of(2019, 9, 7),
                LocalDate.of(2019, 10, 12),
                LocalDate.of(2019, 11, 2),
                LocalDate.of(2019, 11, 15),
                LocalDate.of(2019, 11, 20),
                LocalDate.of(2019, 12, 25));
        HOLIDAYS = Collections.unmodifiableSet(new HashSet<>(dates));
    }

    private static final String CHROME_DRIVER_NAME = "webdriver.chrome.driver";
    private static final String GP_URL = "https://helpdesk.tqi.com.br/sso/login.action?urlRedirectApp=https://helpdesk.tqi.com.br/tqiextranet/autenticacao.asp";

    @Value("${gpuser}")
    private String user;
    @Value("${pass}")
    private String pass;
    @Value("${worked_hours}")
    private String workedHours;
    @Value("${start_date}")
    private String startDate;
    @Value("${end_date}")
    private String endDate;

    public void run() {
        try {
            WebDriver driver = getWebDriver();
            doLogin(driver);
            inputHours(driver);
            driver.close();
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
            String year = String.format("yearDiv%s", date.getYear() - 1);
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
            driver.findElement(By.xpath("/html/body/div[10]/form/table[1]/tbody/tr[3]/td[2]/select/option[15]"));
            driver.findElement(By.xpath("/html/body/div[10]/form/table[1]/tbody/tr[3]/td[2]/select/option[15]")).click();
            driver.findElement(By.name("horas_trab")).sendKeys(workedHours);
            driver.findElement(By.name("BtGravar")).click();
        }
    }

    private WebDriver getWebDriver() {

        String currentOS = System.getProperty("os.name");

        String driverPath;

        if (currentOS.equalsIgnoreCase("linux")) {
            driverPath = new File("src/main/resources/driver/chromedriver_linux").getAbsolutePath();
        } else if (currentOS.equalsIgnoreCase("windows")) {
            driverPath = new File("src/main/resources/driver/chromedriver.exe").getAbsolutePath();
        } else if (currentOS.equalsIgnoreCase("mac os")) {
            driverPath = new File("src/main/resources/driver/chromedriver_mac").getAbsolutePath();
        } else {
            throw new RuntimeException("Sistema operacional não suportado.");
        }

        // create a Chrome Web Driver
        System.setProperty(CHROME_DRIVER_NAME, driverPath);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");
        WebDriver driver = new ChromeDriver();
        driver.get(GP_URL);
        return driver;
    }

    private void doLogin(final WebDriver driver) throws InterruptedException {
        driver.findElement(By.id("userName")).sendKeys(user);
        driver.findElement(By.id("userPass")).sendKeys(pass);
        driver.findElement(By.id("submitLogin")).click();
        while (!driver.getCurrentUrl().contains("https://helpdesk.tqi.com.br/tqiextranet/extranet.asp")) {
            Thread.sleep(2000);
        }
    }

    private List<LocalDate> getDatesBetween(
            LocalDate startDate, LocalDate endDate) {

        long numOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1L;
        if (numOfDaysBetween > 0) {
            return IntStream.iterate(0, i -> i + 1)
                    .limit(numOfDaysBetween)
                    .mapToObj(startDate::plusDays)
                    .filter(f -> !WEEKEND.contains(f.getDayOfWeek().getValue()) && !HOLIDAYS.contains(f))
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(startDate);
        }

    }
}
