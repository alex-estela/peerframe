package fr.estela.peerframe.device.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ParsingUtil {

    private static DateFormat DF_ISO8601UTC;
    static {
        DF_ISO8601UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        DF_ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void printPrettyJson(Logger logger, String json) {
        try {
            if (logger.isDebugEnabled()) {
                ObjectMapper mapper = new ObjectMapper();
                logger.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readValue(json, Object.class)));                
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean jsonNodeExists(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull();
    }

    public static String toISO8601UTCString(Date date) {
        return DF_ISO8601UTC.format(date);
    }

}
