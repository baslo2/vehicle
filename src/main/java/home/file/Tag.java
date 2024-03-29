package home.file;

public enum Tag {

    COLOR("color"),
    DATE("date"),
    HAS_CRADLE("has_cradle"),
    HAS_TRAILER("has_trailer"),
    IS_TRANSPORTS_CARGO("is_transtorts_cargo"),
    IS_TRANSPORTS_PASSENGERS("is_transpotrs_passengers"),
    NUMBER("number"),
    TYPE("type"),
    VEHICLE("vehicle"),
    VEHICLES("vehicles");

    private final String tagName;

    private Tag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public static Tag getTag(String tagName) {
        String tagFarmatted = tagName.strip();
        for (Tag tag : Tag.values()) {
            if (tagFarmatted.equals(tag.getTagName())) {
                return tag;
            }
        }
        return null;
    }
}
