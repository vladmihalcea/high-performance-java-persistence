package com.vladmihalcea.book.hpjp.spring.transaction.readonly.service;

import com.vladmihalcea.book.hpjp.spring.transaction.readonly.config.stats.SpringTransactionStatistics;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.domain.Product;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.repository.ProductRepository;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.service.fxrate.FxCurrency;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.service.fxrate.FxRate;
import com.vladmihalcea.book.hpjp.spring.transaction.readonly.service.fxrate.FxRateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;

    private ProductRepository productRepository;

    public ProductService(
        @Autowired RestTemplate restTemplate,
        @Autowired ProductRepository productRepository) {
        this.restTemplate = restTemplate;
        this.productRepository = productRepository;
    }

    public Product findById(Long id, FxCurrency currency) {
        FxRate fxRate = getFxRate();
        Product product = productRepository.findById(id).orElseThrow();
        FxCurrency productCurrency = product.getCurrency();
        if (!productCurrency.equals(currency)) {
            product.convertTo(currency, fxRate);
        }
        return product;
    }

    @Transactional
    public Product findByIdReadWrite(Long id, FxCurrency currency) {
        return findById(id, currency);
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
        SpringTransactionStatistics.report().fxRateTime(System.nanoTime() - startNanos);
        return fxRate;
    }
}
