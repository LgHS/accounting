package be.lghs.accounting.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Data
@ConfigurationProperties("lghs.accounting")
public class AccountingConfiguration {

    private String codaRs;
    private BigDecimal averageMonthlyRent;
}
