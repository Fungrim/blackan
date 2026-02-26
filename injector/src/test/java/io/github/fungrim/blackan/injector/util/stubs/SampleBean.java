package io.github.fungrim.blackan.injector.util.stubs;

public class SampleBean {

    public String name;
    private Integer count;

    public SampleBean() {
    }

    public SampleBean(String name, Integer count) {
        this.name = name;
        this.count = count;
    }

    public void init(String value) {
    }

    public void setup(String name, Integer count) {
    }
}
