package com.library.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

// Sin filtro fijo: se ejecutan todas las suites. Para acotar la corrida se usa
// -Dcucumber.filter.tags="@funcional" o "@integracion" desde Maven.
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.library",
        monochrome = true)
public class RunApiTest {
}
