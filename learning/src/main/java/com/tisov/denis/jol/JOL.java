package com.tisov.denis.jol;

import com.tisov.denis.padding.LongWrapper;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;

public class JOL {

    public static void show() {
        IO.println(GraphLayout.parseInstance(new LongWrapper()).toFootprint());
        IO.println(ClassLayout.parseClass(LongWrapper.class).toPrintable());
    }

}
