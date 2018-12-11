package guru.springframework.e2e;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.html.HTMLInputElement;

import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;

import io.github.bonigarcia.wdm.WebDriverManager;

public class WebBrowserTest {

	protected static final Logger logger = LoggerFactory.getLogger(WebBrowserTest.class);
	protected static WebDriver driver;

	@Before
	public void beforeEach() throws MalformedURLException, InterruptedException {
		
		String sutUrl = System.getenv("SUT_URL");
		
		if(sutUrl == null) {
			sutUrl = System.getProperty("sutURL");
		}
		
		if(sutUrl == null) {
			sutUrl = "http://localhost:8080";
		}
		
		logger.info("Webapp URL: " + sutUrl);

		WebDriverManager.chromedriver().setup();
		driver = new ChromeDriver();
				
		driver.get(sutUrl);
		
	}
	
	@Test
	public void newProductTest() {
		
		WebElement link = driver.findElement(By.linkText("New Product"));
		
		link.click();
		
		WebElement description = driver.findElement(By.id("description"));
		description.sendKeys("Description");
		
		WebElement price = driver.findElement(By.id("price"));
		price.sendKeys("4000");
			
		WebElement button = driver.findElement(By.tagName("button"));
		button.click();
		
		WebElement title = driver.findElement(By.tagName("h2"));
		
		assertEquals(title.getText(),"Show Product");		
	}

	@After
	public void afterEach() {
		logger.info("Shutting down browser...");
		driver.quit();
	}
}