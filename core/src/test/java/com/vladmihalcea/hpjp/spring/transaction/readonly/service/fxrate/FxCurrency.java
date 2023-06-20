package com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate;

import java.util.Locale;

/**
 * @author Vlad Mihalcea
 */
public enum FxCurrency {
    USD("American dollar"),
    EUR("EURO"),
    GBP("Pound sterling"),
    CHF("Swiss Franc"),
    DKK("Danish rigsdaler"),
    SEK("Swedish riksdaler"),
    NOK("Norwegian speciedaler"),
    HUF("Hungarian pengő"),
    CZK("Czech koruna"),
    PLN("Polish marka"),
    BGN("Bulgarian lev"),
    HRK("Croatian kuna"),
    RON("Romanian leu"),
    ;

    private String description;

    FxCurrency(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static FxCurrency resolve(String token) {
        FxCurrency fxCurrency = resolveOrNull(token);
        if(fxCurrency != null) {
            return fxCurrency;
        } else {
            throw new IllegalArgumentException(
                String.format("The [%s] currency is not supported!", token)
            );
        }
    }

    public static FxCurrency resolveOrNull(String token) {
        for(FxCurrency currency : values()) {
            if(currency.name().toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT))) {
                return currency;
            }
        }
        return null;
    }

    public static FxCurrency ofCountryCode(String countryCode) {
        switch (countryCode) {
            case "AT":
            case "BE":
            case "CY":
            case "EE":
            case "ES":
            case "FI":
            case "FR":
            case "DE":
            case "GR":
            case "IE":
            case "IT":
            case "LV":
            case "LT":
            case "LU":
            case "MT":
            case "NL":
            case "PT":
            case "SK":
            case "SI":
                return EUR;
            case "BG":
                return BGN;
            case "HR":
                return HRK;
            case "CZ":
                return CZK;
            case "DK":
                return DKK;
            case "GB":
                return GBP;
            case "HU":
                return HUF;
            case "CH":
            case "LI":
                return CHF;
            case "NO":
                return NOK;
            case "PL":
                return PLN;
            case "RO":
                return RON;
            case "SE":
                return SEK;
            case "US":
                return USD;
            default:
                return null;
        }
    }
}
