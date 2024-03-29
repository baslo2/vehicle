package home.file.json_yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.Storage;
import home.file.Tag;
import home.models.AbstractVehicle;
import home.utils.LogUtils;

public final class JsonExporter extends AbstractJsonYamlExporter {

    private static final Logger LOG = LoggerFactory.getLogger(JsonExporter.class);

    @Override
    public String exportAllDataObjsToString() {
        try {
            var convertedDataObjs = new ArrayList<Map<String, String>>();
            for (AbstractVehicle dataObj : Storage.INSTANCE.getAll()) {
                convertedDataObjs.add(convertDataObjToMap(dataObj));
            }

            var dataMap = new HashMap<String, List<Map<String, String>>>();
            dataMap.put(Tag.VEHICLES.getTagName(), convertedDataObjs);

            ObjectMapper objectMapper = new ObjectMapper();
            String dataObjsInJsonFormat = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataMap);
            return dataObjsInJsonFormat;
        } catch (JsonProcessingException e) {
            throw LogUtils.logAndCreateIllegalStateException("JSON converter error", LOG, e);
        }
    }
}
