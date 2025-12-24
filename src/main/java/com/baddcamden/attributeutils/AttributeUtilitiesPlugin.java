package com.baddcamden.attributeutils;

public final class AttributeUtilitiesPlugin {
    private static final AttributeUtilitiesPlugin INSTANCE = new AttributeUtilitiesPlugin();
    private final AttributeFacade attributeFacade = new AttributeFacade();

    private AttributeUtilitiesPlugin() {
    }

    public static AttributeUtilitiesPlugin getInstance() {
        return INSTANCE;
    }

    public AttributeFacade getAttributeFacade() {
        return attributeFacade;
    }
}
