package home.models;

import java.util.Objects;

public final class Truck extends AbstractVehicleWithTrailer {

    private static final long serialVersionUID = 7097911260398903331L;

    private boolean isTransportsCargo;

    @Override
    protected VehicleType getInitializedType() {
        return VehicleType.TRUCK;
    }

    public boolean isTransportsCargo() {
        return isTransportsCargo;
    }

    public void setTransportsCargo(boolean isTransportsCargo) {
        this.isTransportsCargo = isTransportsCargo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(isTransportsCargo);
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

        if (!(obj instanceof Truck)) {
            return false;
        }

        Truck other = (Truck) obj;
        return isTransportsCargo == other.isTransportsCargo;
    }
}
