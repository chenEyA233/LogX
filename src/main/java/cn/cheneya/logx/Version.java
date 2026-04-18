package cn.cheneya.logx;

/**
 * Defines the version information for the LogX library.
 *
 * <p>This class provides a centralized location for version tracking,
 * following semantic versioning principles (Year.Major.Minor).
 *
 * @author ChenEya
 * @version 2026.4.0
 */
public final class Version {

    /**
     * The current version of the LogX library.
     * Format: YYYY.Major.Minor
     *
     * <ul>
     *   <li>YYYY: Release year</li>
     *   <li>Major: Major version number (breaking changes)</li>
     *   <li>Minor: Minor version number (backward compatible features)</li>
     * </ul>
     */
    public static final String VERSION = "2026.5.0";

    /**
     * Private constructor to prevent instantiation.
     * This class is a utility class containing only static constants.
     */
    private Version() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Returns the version string of the LogX library.
     *
     * @return the version string
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Checks if the current version matches the specified version string.
     *
     * @param version the version string to compare
     * @return true if versions match, false otherwise
     */
    public static boolean isVersion(String version) {
        return VERSION.equals(version);
    }
}
