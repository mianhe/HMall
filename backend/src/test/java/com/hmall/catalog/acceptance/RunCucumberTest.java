package com.hmall.catalog.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber 验收测试入口：运行 features 下所有场景。
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.hmall.catalog.acceptance,com.hmall.user.acceptance")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/reports/cucumber.html")
public class RunCucumberTest {
}
