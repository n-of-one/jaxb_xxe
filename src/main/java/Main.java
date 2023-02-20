import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

public class Main {

    public static String XML_DEFAULT = "<person><name>test</name></person>";

    public static String XML_EXPANSION = "<!DOCTYPE lolz [\n" +
            " <!ENTITY lol \"lol\"><!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">]>" +
            "<person><name>&lol1;</name></person>";

    public static String XML_FILE = "<!DOCTYPE filez [\n" +
            " <!ENTITY secret SYSTEM \"file:secret.txt\">]>" +
            "<person><name>&secret;</name></person>";

    public static void main(String[] args) throws Exception {

        System.out.println("\n----------------------------------------");
        System.out.println("Baseline: jaxb unmarshalling - manually made unsafe");
        parseRegularXml(readXmlUnsafe(XML_DEFAULT));
        parseExpansionXml(readXmlUnsafe(XML_EXPANSION));
        parseFileLookupXml(readXmlUnsafe(XML_FILE));


        System.out.println("\n----------------------------------------");
        System.out.println("Plain JAXB unmarshalling, no features set.");
        parseRegularXml(readXml(XML_DEFAULT));
        parseExpansionXml(readXml(XML_EXPANSION));
        parseFileLookupXml(readXml(XML_FILE));

        System.out.println("\n-----------------------------------------------------------");
        System.out.println("JAXB using SAXParser with OWASP recommended features");
        parseRegularXml(readXmlOwaspRecommendation(XML_DEFAULT));
        parseExpansionXml(readXmlOwaspRecommendation(XML_EXPANSION));
        parseFileLookupXml(readXmlOwaspRecommendation(XML_FILE));

        System.out.println("\n-------------------------------------------------------");
        System.out.println("JAXB using SAXParser with only 'disallow-doctype-decl'");
        parseRegularXml(readXmlDisallowDoctType(XML_DEFAULT));
        parseExpansionXml(readXmlDisallowDoctType(XML_EXPANSION));
        parseFileLookupXml(readXmlDisallowDoctType(XML_FILE));
    }

    public static void parseRegularXml(String name) {
        String result = ("test".equals(name)) ? "safe" : "unsafe";
        System.out.println("\nNormal XML parsing: " + result);
        System.out.println("  result=" + name);
        System.out.println("  expect=test");
    }

    public static void parseExpansionXml(String name) {
        String result = ("lollollollollollollollollollol".equals(name)) ? "unsafe" : "safe";

        System.out.println("\nXXE Expansion (billion laughs): " + result);
        System.out.println("  result=" + name);
        System.out.println("  expect=(some kind of exception). (if you see lololololol... then lol was expanded)");
    }


    public static void parseFileLookupXml(String name) {
        String result = ("secret_code1234".equals(name)) ? "unsafe" : "safe";
        System.out.println("\nXXE retrieve files: " + result);
        System.out.println("  result=" + name);
        System.out.println("  expect=(some kind of exception) If you see `secret_code1234` then the file was stolen");
    }

    public static String readXml(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(Person.class);
            return ((Person) context
                    .createUnmarshaller()
                    .unmarshal(new StringReader(xml))
            ).getName();
        } catch (Exception exception) {
            return exception.getClass().toString();
        }
    }

    /**
     * Deliberately disable all XXE prevention.
     */
    public static String readXmlUnsafe(String xml) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        return parseUsingSaxParser(xml, spf);
    }


    /**
     * Use recommended setting from: https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxb-unmarshaller
     */
    public static String readXmlOwaspRecommendation(String xml) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        spf.setXIncludeAware(false);

        return parseUsingSaxParser(xml, spf);
    }

    /**
     * Use only the disallow-doctype-decl feature.
     */
    public static String readXmlDisallowDoctType(String xml) throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        return parseUsingSaxParser(xml, spf);
    }

    private static String parseUsingSaxParser(String xml, SAXParserFactory spf) {
        try {
            Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(new StringReader(xml)));
            JAXBContext jc = JAXBContext.newInstance(Person.class);
            Unmarshaller um = jc.createUnmarshaller();
            return ((Person) um.unmarshal(xmlSource)).getName();
        } catch (Exception exception) {
            return exception.getClass().toString();
        }
    }
}