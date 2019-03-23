package com.cosmo.kite.simulcast.pages;

import com.cosmo.kite.simulcast.LoopbackStats;
import org.apache.log4j.Logger;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import java.util.List;

import static com.cosmo.kite.util.WebDriverUtils.executeJsScript;

public class MedoozeLoopbackPage extends SimulcastPageBase {


  @FindBy(id="s_s_w")
  private List<WebElement> gaugesWidth;

  @FindBy(id="s_s_h")
  private List<WebElement> gaugesHeight;

  @FindBy(id="s_s_f")
  private List<WebElement> gaugesFPS;

  @FindBy(id="s_s_b")
  private List<WebElement> gaugesBW;

  @FindBy(tagName="button")
  private List<WebElement> buttons;

  public MedoozeLoopbackPage(WebDriver webDriver) {
    super(webDriver);
    PageFactory.initElements(webDriver, this);
  }

  /**
   *
   * Click a button
   *
   * @param rid the rid
   * @param tid the tid
   */
  @Override
  public void clickButton(String rid, int tid) {
    for(WebElement b:buttons) {
      if (b.getAttribute("data-rid").equalsIgnoreCase(rid)
          && b.getAttribute("data-tid").equalsIgnoreCase("" + tid)) {
        b.sendKeys(Keys.ENTER);
        break;
      }
    }
  }

  /**
   * Gets the LoopbackStats
   *
   * @return LoopbackStats
   */
  @Override
  public LoopbackStats getLoopbackStats() {
    return new LoopbackStats(
        gaugesWidth.get(0).getText(),
        gaugesHeight.get(0).getText(),
        gaugesFPS.get(0).getText(),
        gaugesBW.get(0).getText(),
        gaugesWidth.get(1).getText(),
        gaugesHeight.get(1).getText(),
        gaugesFPS.get(1).getText(),
        gaugesBW.get(1).getText());
  }

}
