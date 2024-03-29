package home.models;

import java.util.Objects;

public final class Car extends AbstractVehicleWithTrailer {

    private static final long serialVersionUID = 3025869662456388766L;

    private boolean isTransportsPassengers;

    @Override
    protected VehicleType getInitializedType() {
        return VehicleType.CAR;
    }

    public boolean isTransportsPassengers() {
        return isTransportsPassengers;
    }

    public void setTransportsPassengers(boolean isTransportsPassengers) {
        this.isTransportsPassengers = isTransportsPassengers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(isTransportsPassengers);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof Car)) {
            return false;
        }

        Car other = (Car) obj;
        return isTransportsPassengers == other.isTransportsPassengers;
    }
}
