# Fulfillment handling differences

AttributeItemUtils ships two separate pipelines for adding attribute modifiers to items:

* The vendored **AttributeUtils** flow (e.g., `ItemAttributeHandler#buildAttributeItem`) stores every requested attribute in the item's `PersistentDataContainer` with the original `TriggerCriterion` and slot binding. When a later "attribute reset" runs, AttributeUtils can rebuild the modifiers from that persistent data, so items survive resets even if all live modifiers are wiped first.
* The newer **AttributeItemUtils plugin** flow (see `AttributeService#applyAttributes`) directly calls `AttributeFacade#refresh`/`mutate` and never records the requested attributes in persistent data. Because nothing is persisted, any reset that clears `AttributeModifiers` removes the plugin-added entries permanently, which is why dropped kit items lose their bonuses while `/attributeitem`-generated items do not.

To match AttributeUtils behavior for fulfillment requirements, kit-generated items need to be authored through the AttributeUtils pipeline (or mirror its persistent data writes) so that slot/criterion metadata is saved and can be reapplied after a reset.
