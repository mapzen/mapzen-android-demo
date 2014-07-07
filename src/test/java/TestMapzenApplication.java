package com.mapzen;

import com.mapzen.core.CommonModule;
import com.mapzen.core.TestAppModule;

import java.util.Arrays;
import java.util.List;

public class TestMapzenApplication extends MapzenApplication {
    @Override
    protected List<Object> getModules() {
        return Arrays.asList(
                new CommonModule(this),
                new TestAppModule(this)
        );
    }
}
