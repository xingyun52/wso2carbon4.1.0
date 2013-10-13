package org.wso2.carbon.appfactory.jenkins.build;

/**
 * This Enum defines all possible build results in Jenkins CI
 * 
 */
public enum BuildResult {

    Succesfull("SUCCESS", "Successful"), Aborted("ABORTED", "Aborted"), Failed("FAILURE", "Failed"),
    Unstable("UNSTABLE", "Unstable"), NotBuild("NOT_BUILT", "Not Build");

    /**
     * The Id returned by the jenkins CI
     */
    private String id;

    /**
     * Display friendly name of the build result
     */
    private String name;

    private BuildResult(String id, String name) {
        this.id = id;
        this.name = name;

    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    /**
     * Returns the {@link BuildResult} matching the given string
     * 
     * @param value
     *            the {@link String} representation of the build result
     * @return matching {@link BuildResult} or null ( if match doesn't occur
     */
    public static BuildResult convert(String value) {
        for (BuildResult bs : BuildResult.values()) {
            if (bs.getId().equalsIgnoreCase(value)) {
                return bs;
            }
        }
        return null;
    }

}
