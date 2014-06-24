package gov.nist.scap.creator.enums;

public enum ComponentType {
    //Do not rearrange
    XCCDF("xccdf.xml"),
    OVAL("oval.xml"),
    PATCHES("patches.xml"),
    CPE_OVAL("cpe-oval.xml"),
    CPE_DICT("cpe-dictionary.xml");

    private String ending;

    private ComponentType(String ending) {
        this.ending = ending;
    }

    public String getEnding() {
        return ending;
    }

    public static ComponentType getType(String name) {
        ComponentType result = null;
        for (ComponentType e : ComponentType.values()) {
            if (name.endsWith(e.getEnding())) {
                result = e;
            }
        }
        return result;
    }
}
