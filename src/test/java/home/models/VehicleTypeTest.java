package home.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class VehicleTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "car",
            "truck",
            "motorcycle",

            // check ignore case
            "CAR",
            "trUck",
            "MotorCycle"
    })
    void existedType(String type) {
        assertNotNull(VehicleType.getVehicleType(type));
    }

    @Test
    void notExistedType() {
        assertNull(VehicleType.getVehicleType("notExistedType"));
    }

    @Disabled("chech how work 'skip test'")
    @Test
    void someTest() {
        String expected = "some value";
        String actual = "some value";
        assertEquals(expected, actual);
    }
}
