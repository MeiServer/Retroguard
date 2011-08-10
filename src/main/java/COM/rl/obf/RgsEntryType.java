package COM.rl.obf;

public enum RgsEntryType
{
    OPTION(".option"),
    ATTR(".attribute"),
    CLASS(".class"),
    NOT_CLASS("!class"),
    METHOD(".method"),
    NOT_METHOD("!method"),
    FIELD(".field"),
    NOT_FIELD("!field"),
    PACKAGE_MAP(".package_map"),
    REPACKAGE_MAP(".repackage_map"),
    CLASS_MAP(".class_map"),
    METHOD_MAP(".method_map"),
    FIELD_MAP(".field_map"),
    NOWARN(".nowarn");

    public String directive;

    RgsEntryType(String directive)
    {
        this.directive = directive;
    }

    int length()
    {
        return this.directive.length();
    }
}
