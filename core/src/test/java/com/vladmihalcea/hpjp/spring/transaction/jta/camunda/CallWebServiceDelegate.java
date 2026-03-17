package com.vladmihalcea.hpjp.spring.transaction.jta.camunda;

import com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate.FxRate;
import com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate.FxRateUtil;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Camunda JavaDelegate that calls a web service.
 *
 * @author Vlad Mihalcea
 */
@Component("callWebServiceDelegate")
public class CallWebServiceDelegate implements JavaDelegate {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Get FXRate from web service");

        FxRate fxRate = getFxRate();

        execution.setVariable("fxRate", fxRate);
    }

    private FxRate getFxRate() {
        long startNanos = System.nanoTime();
        String fxRateXmlString = restTemplate.getForObject(FxRateUtil.FX_RATE_XML_URL, String.class);
        FxRate fxRate = null;
        if (fxRateXmlString != null) {
            fxRate = FxRateUtil.parseFxRate(
                fxRateXmlString.getBytes(
                    StandardCharsets.UTF_8
                )
            );
        }
        LOGGER.debug(
            "FxRate loading took: [{}] ms",
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        );
        return fxRate;
    }
}

