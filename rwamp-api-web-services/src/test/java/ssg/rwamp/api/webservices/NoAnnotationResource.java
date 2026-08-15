package ssg.rwamp.api.webservices;

/**
 * A resource without any annotations - should NOT be handled by the provider.
 */
public class NoAnnotationResource {

    public String doSomething(String input) {
        return "result:" + input;
    }
}
