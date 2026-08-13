package ssg.rwamp.feature.registration;

/**
 * Utility for matching procedure URIs against patterns.
 * <p>
 * Supports three match modes compatible with WAMP Advanced Profile:
 * <ul>
 *   <li><b>exact</b> — strict string equality</li>
 *   <li><b>prefix</b> — the registered pattern is a prefix of the procedure URI</li>
 *   <li><b>wildcard</b> — {@code *} matches a single URI segment (dot-delimited)</li>
 * </ul>
 *
 * @since 0.1.0
 */
final class PatternMatcher {

    private PatternMatcher() {}

    /**
     * Checks if a procedure URI matches a registered pattern.
     *
     * @param matchType "exact", "prefix", or "wildcard"
     * @param pattern   the registered pattern URI
     * @param procedure the incoming procedure URI
     * @return {@code true} if the procedure matches the pattern
     */
    public static boolean matches(String matchType, String pattern, String procedure) {
        return switch (matchType) {
            case "exact" -> pattern.equals(procedure);
            case "prefix" -> procedure.startsWith(pattern);
            case "wildcard" -> wildcardMatch(pattern, procedure);
            default -> false;
        };
    }

    /**
     * Wildcard matching: {@code *} matches exactly one dot-delimited segment.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code com.example.*} matches {@code com.example.foo} but not {@code com.example.foo.bar}</li>
     *   <li>{@code com.*.bar} matches {@code com.example.bar}</li>
     *   <li>{@code com.*.*} matches {@code com.example.foo}</li>
     * </ul>
     */
    private static boolean wildcardMatch(String pattern, String procedure) {
        var pParts = pattern.split("\\.", -1);
        var sParts = procedure.split("\\.", -1);

        if (pParts.length != sParts.length) return false;

        for (int i = 0; i < pParts.length; i++) {
            if (!"*".equals(pParts[i]) && !pParts[i].equals(sParts[i])) {
                return false;
            }
        }
        return true;
    }
}
