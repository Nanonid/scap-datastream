package gov.nist.scap.creator.helpers;

import gov.nist.scap.creator.enums.ComponentType;

import java.util.Comparator;

public class ComponentOrderComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        ComponentType o1e = ComponentType.getType(o1);
        ComponentType o2e = ComponentType.getType(o2);
        return o1e == o2e ? 0 : (o1e.ordinal() < o2e.ordinal() ? -1 : 1);
    }
}
