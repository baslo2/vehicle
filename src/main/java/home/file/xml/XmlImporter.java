package home.file.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.file.IImporter;
import home.file.Tag;
import home.models.AbstractVehicle;
import home.models.AbstractVehicleWithTrailer;
import home.models.Car;
import home.models.Motorcycle;
import home.models.Truck;
import home.models.VehicleType;
import home.utils.LogUtils;
import home.utils.Utils;

public final class XmlImporter implements IImporter {

    private static final Logger LOG = LoggerFactory.getLogger(XmlImporter.class);

    @Override
    public List<AbstractVehicle> importDataObjsFromFile(File file) {
        try (var fileInputSream = new FileInputStream(file);
             var xmlReaderAutoCloseWrapper = new XmlReaderAutoCloseWrapper(XMLInputFactory
                     .newInstance().createXMLStreamReader(fileInputSream))) {
            XMLStreamReader reader = xmlReaderAutoCloseWrapper.reader();

            var dataObjs = new ArrayList<AbstractVehicle>();
            AbstractVehicle dataObj = null;

            while (reader.hasNext()) {
                int currentParseEvent = reader.next();
                if (XMLStreamConstants.START_ELEMENT == currentParseEvent) {
                    //// Create and fill data object (orientation by opening tag)

                    String tagName = reader.getName().getLocalPart();

                    Tag tag = Tag.getTag(tagName);
                    if (tag == null) {
                        throw new IllegalArgumentException("Wrong start of tag name : " + tagName);
                    }

                    switch (tag) {
                        case VEHICLES:
                            // $FALL-THROUGH$
                        case TYPE:
                            // TYPE in xml is attribute of VEHICLE tag
                            break;

                        case VEHICLE:
                            checkAttribut(reader);
                            dataObj = createDataObj(reader);
                            break;

                        case COLOR:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            dataObj.setColor(reader.getText());
                            break;

                        case NUMBER:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            dataObj.setNumber(reader.getText());
                            break;

                        case DATE:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            String dateTime = reader.getText();
                            dataObj.setDateTime(Utils.getLongFromFormattedDate(dateTime));
                            break;

                        case HAS_TRAILER:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            if (dataObj instanceof AbstractVehicleWithTrailer vehicleWithTrailer) {
                                boolean hasTrailer = Boolean.parseBoolean(reader.getText());
                                vehicleWithTrailer.setHasTrailer(hasTrailer);
                            }
                            break;

                        case IS_TRANSPORTS_PASSENGERS:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            if (dataObj instanceof Car car) {
                                boolean isTransportsPassengers = Boolean.parseBoolean(reader.getText());
                                car.setTransportsPassengers(isTransportsPassengers);
                            }
                            break;

                        case IS_TRANSPORTS_CARGO:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            if (dataObj instanceof Truck truck) {
                                boolean isTranspotrsCargo = Boolean.parseBoolean(reader.getText());
                                truck.setTransportsCargo(isTranspotrsCargo);
                            }
                            break;

                        case HAS_CRADLE:
                            currentParseEvent = reader.next();
                            checkEvent(currentParseEvent, reader);
                            if (dataObj instanceof Motorcycle motorcycle) {
                                boolean hasCradle = Boolean.parseBoolean(reader.getText());
                                motorcycle.setHasCradle(hasCradle);
                            }
                            break;

                        default:
                            throw new IllegalArgumentException("There is no processing for " + tagName);
                    }
                } else if (XMLStreamConstants.END_ELEMENT == currentParseEvent) {
                    //// ADD to list created and filled data object (orientation by closing tag).

                    String tagName = reader.getName().getLocalPart();

                    Tag tag = Tag.getTag(tagName);
                    if (tag == null) {
                        throw new IllegalArgumentException("Wrong end of tag name : " + tagName);
                    }

                    if (Tag.VEHICLE == tag) {
                        dataObjs.add(dataObj);
                    }
                }
            }
            return dataObjs;
        } catch (FileNotFoundException e) {
            throw LogUtils.logAndCreateIllegalStateException(
                    "Can't find xml file for import data : " + file.getAbsolutePath(),
                    LOG, e);
        } catch (Exception e) {
            throw LogUtils.logAndCreateIllegalStateException(
                    "Error while reading xml file : " + file.getAbsolutePath(),
                    LOG, e);
        } catch (FactoryConfigurationError e) {
            throw LogUtils.logAndCreateIllegalStateException(
                    "Error while reading xml file : " + file.getAbsolutePath(),
                    LOG, new IllegalStateException(e.getMessage(), e));
        }
    }

    private void checkAttribut(XMLStreamReader reader) {
        int attrCount = reader.getAttributeCount();
        if (attrCount > 1) {
            throw new IllegalArgumentException("Wrong attribute count in tag [%s] : %d"
                    .formatted(reader.getName(), attrCount));
        }

        String attrName = reader.getAttributeLocalName(0).toString();
        if (!Tag.TYPE.getTagName().equals(attrName)) {
            throw new IllegalArgumentException("Incorrect attribute name for tag [%s] : %s"
                    .formatted(reader.getName(), attrName));
        }
    }

    private AbstractVehicle createDataObj(XMLStreamReader reader) {
        String type = reader.getAttributeValue(0);

        VehicleType vehicleType = VehicleType.getVehicleType(type);
        if (vehicleType == null) {
            throw new IllegalArgumentException("Wrong vehicle type received : " + type);
        }

        return switch (vehicleType) {
            case CAR -> new Car();
            case TRUCK -> new Truck();
            case MOTORCYCLE -> new Motorcycle();
        };
    }

    private void checkEvent(int currentParseEvent, XMLStreamReader reader)
            throws XMLStreamException {
        if (XMLStreamConstants.CHARACTERS != currentParseEvent) {
            throw new IllegalArgumentException("Broken xml file: text area error in tag [%s]"
                    .formatted(reader.getName()));
        }
    }

    private record XmlReaderAutoCloseWrapper(XMLStreamReader reader) implements AutoCloseable {

        @Override
        public void close() throws Exception {
            reader.close();
        }
    }
}
