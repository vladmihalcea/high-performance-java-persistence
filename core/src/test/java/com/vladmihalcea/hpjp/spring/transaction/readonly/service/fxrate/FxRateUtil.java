package com.vladmihalcea.hpjp.spring.transaction.readonly.service.fxrate;

import com.vladmihalcea.hpjp.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FxRateUtil {

    public static final String FX_RATE_XML_URL = "https://www.bnr.ro/nbrfxrates.xml";

    public static class Names {
        public static final String CUBE = "Cube";
        public static final String DATE = "date";
        public static final String CURRENCY = "currency";
        public static final String MULTIPLIER = "multiplier";
    }
    
    public static FxRate parseFxRate(byte[] fxRateXmlBytes) {
        FxRate fxRate = new FxRate();
        Document fxRateDocument = XmlUtils.readXmlDocument(fxRateXmlBytes);
        NodeList cubes = fxRateDocument.getElementsByTagName(Names.CUBE);
        if(cubes.getLength() > 0) {
            Node cubeNode = cubes.item(0);
            String date = cubeNode.getAttributes().getNamedItem(Names.DATE).getNodeValue();
            fxRate.setDate(LocalDate.parse(date));
            NodeList rateNodes = cubeNode.getChildNodes();
            for (int j = 0; j < rateNodes.getLength(); j++) {
                Node rateNode = rateNodes.item(j);
                if (!"Rate".equals(rateNode.getNodeName())) {
                    continue;
                }
                NamedNodeMap attributes = rateNode.getAttributes();
                String currency = attributes.getNamedItem(Names.CURRENCY).getNodeValue();
                BigDecimal fxRateValue = new BigDecimal(rateNode.getTextContent());
                Node multiplierAttribute = attributes.getNamedItem(Names.MULTIPLIER);
                if (multiplierAttribute != null) {
                    fxRateValue = new BigDecimal(multiplierAttribute.getNodeValue());
                }
                fxRate.setRate(currency, fxRateValue);
            }
        }
        return fxRate;
    }
}

