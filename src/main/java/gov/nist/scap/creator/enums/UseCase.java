package gov.nist.scap.creator.enums;

public enum UseCase {
    CONFIGURATION("CONFIGURATION"),
    VULN_ONLY("VULNERABILITY_XCCDF_OVAL"),
    OVAL_ONLY("OVAL_ONLY"),
    SYS_INV("SYSTEM_INVENTORY");

    private String string;

    UseCase(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }
}
