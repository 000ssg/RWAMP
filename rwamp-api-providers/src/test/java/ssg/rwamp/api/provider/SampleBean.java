package ssg.rwamp.api.provider;

/**
 * Simple JavaBean for testing getter/setter-based API discovery.
 */
class SampleBean {
    private String name;
    private int count;
    private boolean active;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
